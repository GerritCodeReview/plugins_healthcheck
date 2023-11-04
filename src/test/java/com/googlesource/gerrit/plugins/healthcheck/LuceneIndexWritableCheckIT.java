// Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.AUTH;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.LUCENEINDEXWRITABLE;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.Sandboxed;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.index.account.AccountSchemaDefinitions;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Key;
import com.googlesource.gerrit.plugins.healthcheck.check.LuceneIndexWritableCheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

@UseLocalDisk
@Sandboxed
@TestPlugin(
    name = "healthcheck-test",
    sysModule = "com.googlesource.gerrit.plugins.healthcheck.Module",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
public class LuceneIndexWritableCheckIT extends LightweightPluginDaemonTest {

  private static Gson gson = new Gson();
  private String healthCheckUriPath;
  private HealthCheckConfig config;

  private void disableChecks(String... checks) throws ConfigInvalidException {
    String sections =
        Arrays.stream(checks).map(this::disableCheckSection).collect(Collectors.joining("\n"));
    config.fromText(sections);
  }

  private String disableCheckSection(String check) {
    return String.format("[healthcheck \"%s\"]\n" + "enabled = false", check);
  }

  @Override
  @Before
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    config = plugin.getSysInjector().getInstance(HealthCheckConfig.class);
    disableChecks(AUTH, QUERYCHANGES);
    //    int numChanges = config.getLimit(HealthCheckNames.QUERYCHANGES);
    //    for (int i = 0; i < numChanges; i++) {
    //      createChange("refs/for/master");
    //    }
    //    accountCreator.create(config.getUsername(HealthCheckNames.AUTH));

    healthCheckUriPath =
        String.format(
            "/config/server/%s~status",
            plugin.getSysInjector().getInstance(Key.get(String.class, PluginName.class)));
  }

  @ConfigSuite.Default
  public static Config luceneConfig() {
    Config cfg = new Config();
    cfg.setString("index", null, "type", "lucene");
    return cfg;
  }

  private RestResponse getHealthCheckStatus() throws IOException {
    return adminRestSession.get(healthCheckUriPath);
  }

  @Test
  public void shouldReturnLuceneIndexWritableCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();
    assertCheckResult(getResponseJson(resp), LUCENEINDEXWRITABLE, "passed");
  }

  @Test
  public void shouldFailReturnLuceneIndexWritableCheckWhenLockFileIsMissing() throws Exception {
    LuceneIndexWritableCheck instance =
        plugin.getSysInjector().getInstance(LuceneIndexWritableCheck.class);

    Path lockFile = instance.getDir(AccountSchemaDefinitions.INSTANCE).resolve("write.lock");
    Files.delete(lockFile);

    RestResponse resp = getHealthCheckStatus();
    resp.assertStatus(SC_INTERNAL_SERVER_ERROR);
    assertCheckResult(getResponseJson(resp), LUCENEINDEXWRITABLE, "failed");
  }

  @Test
  public void shouldFailReturnLuceneIndexWritableCheckWhenLockFileIsNotWritable() throws Exception {
    LuceneIndexWritableCheck instance =
        plugin.getSysInjector().getInstance(LuceneIndexWritableCheck.class);

    Path lockFile = instance.getDir(AccountSchemaDefinitions.INSTANCE).resolve("write.lock");
    Set<PosixFilePermission> permissions =
        Files.readAttributes(lockFile, PosixFileAttributes.class).permissions();
    permissions.removeAll(Set.of(OWNER_WRITE, GROUP_WRITE, OTHERS_WRITE));
    Files.setPosixFilePermissions(lockFile, permissions);

    RestResponse resp = getHealthCheckStatus();
    resp.assertStatus(SC_INTERNAL_SERVER_ERROR);
    assertCheckResult(getResponseJson(resp), LUCENEINDEXWRITABLE, "failed");
  }

  private JsonObject getResponseJson(RestResponse resp) throws IOException {
    return gson.fromJson(resp.getReader(), JsonObject.class);
  }

  private void assertCheckResult(JsonObject respPayload, String checkName, String result) {
    assertThat(respPayload.has(checkName)).isTrue();
    JsonObject reviewDbStatus = respPayload.get(checkName).getAsJsonObject();
    assertThat(reviewDbStatus.has("result")).isTrue();
    assertThat(reviewDbStatus.get("result").getAsString()).isEqualTo(result);
  }
}
