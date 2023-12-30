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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.CHANGES_INDEX;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.JGIT;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;

import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.Sandboxed;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "healthcheck-test",
    sysModule =
        "com.googlesource.gerrit.plugins.healthcheck.AbstractHealthCheckIntegrationTest$TestModule",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
@Sandboxed
public class HealthCheckIT extends AbstractHealthCheckIntegrationTest {
  private String failFilePath = "/tmp/fail";

  @Override
  @Before
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    new File(failFilePath).delete();

    // disable `changesindex` check as it requires @UseLocalDisk annotation (so that all operations
    // are persisted to local FS) to be applied and that would degrade this IT performance - see
    // ChangesIndexHealthCheckIT for a changes index dedicated integration tests
    super.disableCheck(CHANGES_INDEX);
  }

  @Test
  public void shouldReturnOkWhenHealthy() throws Exception {
    getHealthCheckStatus().assertOK();
  }

  @Test
  public void shouldReturnOkWhenHealthyAndAnonymousReadIsBlocked() throws Exception {
    blockAnonymousRead();
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
  @GerritConfig(name = "container.replica", value = "true")
  public void shouldReturnJGitCheckForReplicaWhenAuthenticated() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();
    assertCheckResult(getResponseJson(resp), JGIT, "passed");
  }

  @Test
  @GerritConfig(name = "container.replica", value = "true")
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
  @GerritConfig(name = "container.replica", value = "true")
  public void shouldReturnQueryChangesAsDisabledForReplica() throws Exception {
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

  @Test
  public void shouldReturnFailedIfFailFlagFileExists() throws Exception {
    setFailFlagFilePath(failFilePath);
    createFailFileFlag(failFilePath);
    getHealthCheckStatus();
    RestResponse resp = getHealthCheckStatus();
    resp.assertStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    JsonObject respBody = getResponseJson(resp);
    assertThat(respBody.has("reason")).isTrue();
    assertThat(respBody.get("reason").getAsString()).isEqualTo("Fail Flag File exists");
  }

  @Override
  protected void disableCheck(String check) throws ConfigInvalidException {
    // additionally disable `changesindex` healthcheck as it requires @UseLocalDisk annotation to
    // run properly
    config.fromText(
        String.format(
            "[healthcheck \"%s\"]\n enabled = false\n[healthcheck \"%s\"]\n enabled = false",
            check, CHANGES_INDEX));
  }

  private void createFailFileFlag(String path) throws IOException {
    File file = new File(path);
    file.createNewFile();
    file.deleteOnExit();
  }

  private void setFailFlagFilePath(String path) throws ConfigInvalidException {
    config.fromText(String.format("[healthcheck]\n" + "failFileFlagPath = %s", path));
  }
}
