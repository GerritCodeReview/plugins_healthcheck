package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.AUTH;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.LUCENEINDEXWRITABLE;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.index.IndexType;
import com.google.gerrit.server.index.change.ChangeSchemaDefinitions;
import com.google.gerrit.testing.SystemPropertiesTestRule;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Key;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

@UseLocalDisk
@TestPlugin(
    name = "healthcheck-test",
    sysModule = "com.googlesource.gerrit.plugins.healthcheck.Module",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
public class LuceneIndexWritableCheckIT extends LightweightPluginDaemonTest {

  @ClassRule
  public static SystemPropertiesTestRule systemProperties =
      new SystemPropertiesTestRule(IndexType.SYS_PROP, "lucene");

  private static Gson gson = new Gson();
  private static final String CHANGES = ChangeSchemaDefinitions.NAME;
  private Project.NameKey project;
  private String changeId;
  private String healthCheckUriPath;
  private HealthCheckConfig config;

  private void disableCheck(String check) throws ConfigInvalidException {
    config.fromText(disableCheckSection(check) + "\n" + disableCheckSection(QUERYCHANGES));
  }

  private String disableCheckSection(String check) {
    return String.format("[healthcheck \"%s\"]\n" + "enabled = false", check);
  }

  @Override
  @Before
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    config = plugin.getSysInjector().getInstance(HealthCheckConfig.class);
    disableCheck(AUTH);

    healthCheckUriPath =
        String.format(
            "/config/server/%s~status",
            plugin.getSysInjector().getInstance(Key.get(String.class, PluginName.class)));
  }

  //  @ConfigSuite.Default
  //  public static Config luceneConfig() {
  //    Config cfg = new Config();
  //    cfg.setString("index", null, "type", "lucene");
  //    return cfg;
  //  }
  //

  private RestResponse getHealthCheckStatus() throws IOException {
    return adminRestSession.get(healthCheckUriPath);
  }

  @Test
  public void shouldReturnLuceneIndexWritableCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    System.out.println(resp.getEntityContent());
    resp.assertOK();
    assertCheckResult(getResponseJson(resp), LUCENEINDEXWRITABLE, "passed");
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

  //  private void assertReady(int expectedReady) throws Exception {
  //    Set<Integer> allVersions = ChangeSchemaDefinitions.INSTANCE.getSchemas().keySet();
  //    GerritIndexStatus status = new GerritIndexStatus(sitePaths);
  //    assertWithMessage("ready state for index versions")
  //        .that(
  //            allVersions.stream().collect(toImmutableMap(v -> v, v -> status.getReady(CHANGES,
  // v))))
  //        .isEqualTo(allVersions.stream().collect(toImmutableMap(v -> v, v -> v ==
  // expectedReady)));
  //  }
}
