// Copyright (C) 2018 The Android Open Source Project
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
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gson.Gson;
import org.junit.Test;

@TestPlugin(
    name = "healthcheck",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
public class HealthCheckTest extends LightweightPluginDaemonTest {

  @Test
  public void shouldReturnOkWhenHealthy() throws Exception {
    RestResponse resp = adminRestSession.get("/plugins/healthcheck/");
    resp.assertOK();
  }

  @Test
  public void shouldReturnAJsonPayload() throws Exception {
    RestResponse resp = adminRestSession.get("/plugins/healthcheck/");
    assertThat(resp.getHeader("Content-Type")).contains("application/json");
  }

  @Test
  public void shouldReturnReviewDbCheck() throws Exception {
    RestResponse resp = adminRestSession.get("/plugins/healthcheck/");
    Gson gson = new Gson();

    CheckStatus checkStatus = gson.fromJson(resp.getReader(), CheckStatus.class);
    assertThat(checkStatus.reviewdb).isTrue();
  }
}
