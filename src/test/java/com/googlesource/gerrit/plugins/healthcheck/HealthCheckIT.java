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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.REVIEWDB;

import java.io.IOException;

import org.junit.Test;

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@TestPlugin(name = "healthcheck", sysModule = "com.googlesource.gerrit.plugins.healthcheck.Module")
public class HealthCheckIT extends LightweightPluginDaemonTest {
  Gson gson = new Gson();

  @Test
  public void shouldReturnOkWhenHealthy() throws Exception {
    getHealthCheckStatus().assertOK();
  }

  @Test
  public void shouldReturnAJsonPayload() throws Exception {
    assertThat(getHealthCheckStatus().getHeader(CONTENT_TYPE)).contains("application/json");
  }

  @Test
  public void shouldReturnReviewDbCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    JsonObject respPayload = gson.fromJson(resp.getReader(), JsonObject.class);
    assertThat(respPayload.has(REVIEWDB)).isTrue();

    JsonObject reviewDbStatus = respPayload.get(REVIEWDB).getAsJsonObject();
    assertThat(reviewDbStatus.has("result")).isTrue();
    assertThat(reviewDbStatus.get("result").getAsString()).isEqualTo("passed");
  }

  private RestResponse getHealthCheckStatus() throws IOException {
    return adminRestSession.get("/config/server/healthcheck~status");
  }
}
