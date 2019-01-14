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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.Response;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

public class HealthCheckStatusEndpointTest {

  public static class TestHealthCheck implements HealthCheck {
    private final HealthCheck.Status checkResult;
    private final String checkName;

    public TestHealthCheck(String checkName, HealthCheck.Result result, long ts, long elapsed) {
      this.checkName = checkName;
      this.checkResult = new Status(result, ts, elapsed);
    }

    @Override
    public Status run() {
      return checkResult;
    }

    @Override
    public String name() {
      return checkName;
    }
  }

  @Test
  public void shouldReturnOkWhenAllChecksArePassing() throws Exception {
    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                DynamicSet.bind(binder(), HealthCheck.class)
                    .toInstance(new TestHealthCheck("checkOk", HealthCheck.Result.PASSED, 1, 2));
              }
            });

    HealthCheckStatusEndpoint healthCheckApi =
        injector.getInstance(HealthCheckStatusEndpoint.class);
    Response<?> resp = (Response<?>) healthCheckApi.apply(null);

    assertThat(resp.statusCode()).isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  public void shouldReturnServerErrorWhenAtLeastOneCheckIsFailing() throws Exception {
    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                DynamicSet.bind(binder(), HealthCheck.class)
                    .toInstance(new TestHealthCheck("checkOk", HealthCheck.Result.PASSED, 1, 2));
                DynamicSet.bind(binder(), HealthCheck.class)
                    .toInstance(new TestHealthCheck("checkKo", HealthCheck.Result.FAILED, 1, 2));
              }
            });

    HealthCheckStatusEndpoint healthCheckApi =
        injector.getInstance(HealthCheckStatusEndpoint.class);
    Response<?> resp = (Response<?>) healthCheckApi.apply(null);

    assertThat(resp.statusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), new HealthCheckApiModule(), testModule);
  }
}
