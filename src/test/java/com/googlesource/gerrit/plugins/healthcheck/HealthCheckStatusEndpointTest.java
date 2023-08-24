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
import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig.DEFAULT_CONFIG;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.GlobalHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.concurrent.Executors;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

public class HealthCheckStatusEndpointTest {

  @Inject private ListeningExecutorService executor;
  MetricMaker disabledMetricMaker = new DisabledMetricMaker();

  public static class TestHealthCheck extends AbstractHealthCheck {
    private final HealthCheck.Result checkResult;
    private final long sleep;

    public TestHealthCheck(
        HealthCheckConfig config,
        String checkName,
        HealthCheck.Result result,
        long sleep,
        MetricMaker metricMaker) {
      super(
          MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10)),
          config,
          checkName,
          metricMaker);
      this.checkResult = result;
      this.sleep = sleep;
    }

    @Override
    public Result doCheck() {
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
      }
      return checkResult;
    }
  }

  @Test
  public void shouldReturnOkWhenAllChecksArePassing() throws Exception {
    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                HealthCheckConfig config =
                    new HealthCheckConfig("[healthcheck]\n" + "timeout = 20");
                TestHealthCheck passingHealthCheck =
                    new TestHealthCheck(
                        DEFAULT_CONFIG,
                        "checkOk",
                        HealthCheck.Result.PASSED,
                        0,
                        disabledMetricMaker);
                DynamicSet<HealthCheck> healthChecks = new DynamicSet<>();
                healthChecks.add("passingHealthCheck", passingHealthCheck);
                GlobalHealthCheck globalHealthCheck =
                    new GlobalHealthCheck(healthChecks, executor, config, disabledMetricMaker);
                bind(GlobalHealthCheck.class).toInstance(globalHealthCheck);
                bind(HealthCheckConfig.class).toInstance(config);
              }
            });

    HealthCheckStatusEndpoint healthCheckApi =
        injector.getInstance(HealthCheckStatusEndpoint.class);
    Response<?> resp = healthCheckApi.apply(null);

    assertThat(resp.statusCode()).isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  public void shouldReturnServerErrorWhenOneChecksTimesOut() throws Exception {
    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                HealthCheckConfig config =
                    new HealthCheckConfig("[healthcheck]\n" + "timeout = 20");
                TestHealthCheck testHealthCheck =
                    new TestHealthCheck(
                        config, "checkOk", HealthCheck.Result.PASSED, 30, disabledMetricMaker);
                DynamicSet<HealthCheck> healthChecks = new DynamicSet<>();
                healthChecks.add("testHealthCheck", testHealthCheck);
                GlobalHealthCheck globalHealthCheck =
                    new GlobalHealthCheck(healthChecks, executor, config, disabledMetricMaker);
                bind(GlobalHealthCheck.class).toInstance(globalHealthCheck);
                bind(HealthCheckConfig.class).toInstance(config);
              }
            });

    HealthCheckStatusEndpoint healthCheckApi =
        injector.getInstance(HealthCheckStatusEndpoint.class);
    Response<?> resp = healthCheckApi.apply(null);

    assertThat(resp.statusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnServerErrorWhenAtLeastOneCheckIsFailing() throws Exception {
    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                HealthCheckConfig config =
                    new HealthCheckConfig("[healthcheck]\n" + "timeout = 20");
                TestHealthCheck passingHealthCheck =
                    new TestHealthCheck(
                        DEFAULT_CONFIG,
                        "checkOk",
                        HealthCheck.Result.PASSED,
                        0,
                        disabledMetricMaker);

                TestHealthCheck failingHealthCheck =
                    new TestHealthCheck(
                        DEFAULT_CONFIG,
                        "checkKo",
                        HealthCheck.Result.FAILED,
                        0,
                        disabledMetricMaker);

                DynamicSet<HealthCheck> healthChecks = new DynamicSet<>();
                healthChecks.add("passingHealthCheck", passingHealthCheck);
                healthChecks.add("failingHealthCheck", failingHealthCheck);
                GlobalHealthCheck globalHealthCheck =
                    new GlobalHealthCheck(healthChecks, executor, config, disabledMetricMaker);
                bind(GlobalHealthCheck.class).toInstance(globalHealthCheck);
                bind(HealthCheckConfig.class).toInstance(config);
              }
            });

    HealthCheckStatusEndpoint healthCheckApi =
        injector.getInstance(HealthCheckStatusEndpoint.class);
    Response<?> resp = healthCheckApi.apply(null);

    assertThat(resp.statusCode()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(
        new HealthCheckExtensionApiModule(), new HealthCheckApiModule(), testModule);
  }
}
