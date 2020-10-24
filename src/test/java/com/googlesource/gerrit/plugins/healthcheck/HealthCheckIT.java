// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.ACTIVEWORKERS;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.AUTH;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.JGIT;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;

import com.google.gerrit.acceptance.GerritConfig;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.Sandboxed;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "healthcheck",
    sysModule = "com.googlesource.gerrit.plugins.healthcheck.Module",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
@Sandboxed
public class HealthCheckIT extends LightweightPluginDaemonTest {
  Gson gson = new Gson();
  HealthCheckConfig config;

  @Override
  @Before
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    config = plugin.getSysInjector().getInstance(HealthCheckConfig.class);
    int numChanges = config.getLimit(HealthCheckNames.QUERYCHANGES);
    for (int i = 0; i < numChanges; i++) {
      createChange("refs/for/master");
    }
    accountCreator.create(config.getUsername(HealthCheckNames.AUTH));
  }

  @Test
  public void shouldReturnOkWhenHealthy() throws Exception {
    getHealthCheckStatus().assertOK();
  }

  @Test
  public void shouldReturnAJsonPayload() throws Exception {
    assertThat(getHealthCheckStatus().getHeader(CONTENT_TYPE)).contains("application/json");
  }

  @Test
  public void shouldReturnJGitCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), JGIT, "passed");
  }

  @Test
  public void shouldReturnJGitCheckAsDisabled() throws Exception {
    disableCheck(JGIT);
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), JGIT, "disabled");
  }

  @Test
  @GerritConfig(name = "container.slave", value = "true")
  public void shouldReturnJGitCheckForReplicaWhenAuthenticated() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();
    assertCheckResult(getResponseJson(resp), JGIT, "passed");
  }

  @Test
  @GerritConfig(name = "container.slave", value = "true")
  public void shouldReturnJGitCheckForReplicaAnonymously() throws Exception {
    RestResponse resp = getHealthCheckStatusAnonymously();
    resp.assertOK();
    assertCheckResult(getResponseJson(resp), JGIT, "passed");
  }

  @Test
  public void shouldReturnAuthCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), AUTH, "passed");
  }

  @Test
  public void shouldReturnAuthCheckAsDisabled() throws Exception {
    disableCheck(AUTH);
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), AUTH, "disabled");
  }

  @Test
  public void shouldReturnQueryChangesCheck() throws Exception {
    createChange("refs/for/master");
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), QUERYCHANGES, "passed");
  }

  @Test
  public void shouldReturnQueryChangesCheckAsDisabled() throws Exception {
    disableCheck(QUERYCHANGES);
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), QUERYCHANGES, "disabled");
  }

  @Test
  @GerritConfig(name = "container.slave", value = "true")
  public void shouldReturnQueryChangesAsDisabledForSlave() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), QUERYCHANGES, "disabled");
  }

  @Test
  public void shouldReturnQueryChangesMultipleTimesCheck() throws Exception {
    createChange("refs/for/master");
    getHealthCheckStatus();
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), QUERYCHANGES, "passed");
  }

  @Test
  public void shouldReturnActiveWorkersCheck() throws Exception {
    createChange("refs/for/master");
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), ACTIVEWORKERS, "passed");
  }

  @Test
  public void shouldReturnActiveWorkersCheckAsDisabled() throws Exception {
    disableCheck(ACTIVEWORKERS);
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), ACTIVEWORKERS, "disabled");
  }

  @Test
  public void shouldReturnActiveWorkersMultipleTimesCheck() throws Exception {
    createChange("refs/for/master");
    getHealthCheckStatus();
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), ACTIVEWORKERS, "passed");
  }

  private RestResponse getHealthCheckStatus() throws IOException {
    return adminRestSession.get("/config/server/healthcheck~status");
  }

  private RestResponse getHealthCheckStatusAnonymously() throws IOException {
    return anonymousRestSession.get("/config/server/healthcheck~status");
  }

  private void assertCheckResult(JsonObject respPayload, String checkName, String result) {
    assertThat(respPayload.has(checkName)).isTrue();
    JsonObject reviewDbStatus = respPayload.get(checkName).getAsJsonObject();
    assertThat(reviewDbStatus.has("result")).isTrue();
    assertThat(reviewDbStatus.get("result").getAsString()).isEqualTo(result);
  }

  private void disableCheck(String check) throws ConfigInvalidException {
    config.fromText(String.format("[healthcheck \"%s\"]\n" + "enabled = false", check));
  }

  private JsonObject getResponseJson(RestResponse resp) throws IOException {
    JsonObject respPayload = gson.fromJson(resp.getReader(), JsonObject.class);
    return respPayload;
  }
}
