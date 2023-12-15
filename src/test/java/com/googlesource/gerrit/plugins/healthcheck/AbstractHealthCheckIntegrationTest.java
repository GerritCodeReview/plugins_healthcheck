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

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Ignore;

@Ignore
public class AbstractHealthCheckIntegrationTest extends LightweightPluginDaemonTest {
  public static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      install(new HealthCheckExtensionApiModule());
      install(new Module());
    }
  }

  protected HealthCheckConfig config;
  protected String healthCheckUriPath;

  private Gson gson = new Gson();

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    config = plugin.getSysInjector().getInstance(HealthCheckConfig.class);

    int numChanges = config.getLimit(HealthCheckNames.QUERYCHANGES);
    for (int i = 0; i < numChanges; i++) {
      createChange("refs/for/master");
    }
    accountCreator.create(config.getUsername(HealthCheckNames.AUTH));

    healthCheckUriPath =
        String.format(
            "/config/server/%s~status",
            plugin.getSysInjector().getInstance(Key.get(String.class, PluginName.class)));
  }

  protected RestResponse getHealthCheckStatus() throws IOException {
    return adminRestSession.get(healthCheckUriPath);
  }

  protected RestResponse getHealthCheckStatusAnonymously() throws IOException {
    return anonymousRestSession.get(healthCheckUriPath);
  }

  protected void assertCheckResult(JsonObject respPayload, String checkName, String result) {
    assertThat(respPayload.has(checkName)).isTrue();
    JsonObject reviewDbStatus = respPayload.get(checkName).getAsJsonObject();
    assertThat(reviewDbStatus.has("result")).isTrue();
    assertThat(reviewDbStatus.get("result").getAsString()).isEqualTo(result);
  }

  protected void disableCheck(String check) throws ConfigInvalidException {
    config.fromText(String.format("[healthcheck \"%s\"]\n" + "enabled = false", check));
  }

  protected JsonObject getResponseJson(RestResponse resp) throws IOException {
    JsonObject respPayload = gson.fromJson(resp.getReader(), JsonObject.class);
    return respPayload;
  }
}
