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
import com.googlesource.gerrit.plugins.healthcheck.check.GlobalHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class HealthCheckStatusEndpoint implements RestReadView<ConfigResource> {

  private final GlobalHealthCheck healthChecks;

  @Inject
  public HealthCheckStatusEndpoint(GlobalHealthCheck healthChecks) {
    this.healthChecks = healthChecks;
  }

  @Override
  public Response<Map<String, Object>> apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    Map<String, Object> result = healthChecks.run();

    result.put("ts", healthChecks.getGlobalStatusSummary().ts);
    result.put("elapsed", healthChecks.getGlobalStatusSummary().elapsed);
    return Response.withStatusCode(getHTTPResultCode(result), result);
  }

  private int getHTTPResultCode(Map<String, Object> result) {
    return healthChecks.getResultStatus(result) == HealthCheck.Result.FAILED ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK;
  }

}
