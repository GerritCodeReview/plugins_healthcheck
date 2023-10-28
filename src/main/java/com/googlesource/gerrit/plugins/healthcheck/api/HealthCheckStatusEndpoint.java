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

package com.googlesource.gerrit.plugins.healthcheck.api;

import com.google.gerrit.extensions.restapi.*;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.check.GlobalHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class HealthCheckStatusEndpoint implements RestReadView<ConfigResource> {

  private final GlobalHealthCheck healthChecks;

  private final String failedFileFlagPath;

  @Inject
  public HealthCheckStatusEndpoint(GlobalHealthCheck healthChecks, HealthCheckConfig config) {
    this.healthChecks = healthChecks;
    this.failedFileFlagPath = config.getFailFileFlagPath();
  }

  @Override
  public Response<Map<String, Object>> apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    if (failFlagFileExists()) {
      return Response.withStatusCode(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Map.of("reason", "Fail Flag File exists"));
    }
    HealthCheck.StatusSummary globalHealthCheckStatus = healthChecks.run();

    Map<String, Object> result = globalHealthCheckStatus.subChecks;
    result.put("ts", globalHealthCheckStatus.ts);
    result.put("elapsed", globalHealthCheckStatus.elapsed);
    return Response.withStatusCode(getHTTPResultCode(globalHealthCheckStatus), result);
  }

  private int getHTTPResultCode(HealthCheck.StatusSummary checkStatus) {
    return checkStatus.result == Result.FAILED
        ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        : HttpServletResponse.SC_OK;
  }

  private boolean failFlagFileExists() throws IOException {
    File file = new File(failedFileFlagPath);
    try (InputStream targetStream = new FileInputStream(file)) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
