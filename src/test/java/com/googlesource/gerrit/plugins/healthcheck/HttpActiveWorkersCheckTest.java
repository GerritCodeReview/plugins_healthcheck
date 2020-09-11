// Copyright (C) 2020 The Android Open Source Project
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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.HTTPACTIVEWORKERS;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.HttpActiveWorkersCheck;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class HttpActiveWorkersCheckTest {

  @Test
  public void shouldPassCheckWhenActiveHttpWorkersLessThanThreshold() {

    MetricRegistry metricRegistry = createHttpMetricRegistry(5);

    Config gerritConfig = new Config();
    gerritConfig.setInt("httpd", null, "maxThreads", 12);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HealthCheckConfig healthCheckConfig =
        new HealthCheckConfig("[healthcheck \"" + HTTPACTIVEWORKERS + "\"]\n" + "  threshold = 50");
    HttpActiveWorkersCheck check = createCheck(injector, healthCheckConfig);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenActiveHttpWorkersMoreThanThreshold() {

    MetricRegistry metricRegistry = createHttpMetricRegistry(6);

    Config gerritConfig = new Config();
    gerritConfig.setInt("httpd", null, "maxThreads", 10);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HealthCheckConfig healthCheckConfig =
        new HealthCheckConfig("[healthcheck \"" + HTTPACTIVEWORKERS + "\"]\n" + "  threshold = 50");
    HttpActiveWorkersCheck check = createCheck(injector, healthCheckConfig);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldFailCheckWhenActiveHttpWorkersMoreThanDefaultThreshold() {

    MetricRegistry metricRegistry = createHttpMetricRegistry(90);
    // By default threshold is 80%
    Config gerritConfig = new Config();
    gerritConfig.setInt("httpd", null, "maxThreads", 100);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HttpActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldPassCheckWhenActiveHttpWorkersLessThanDefaultThreshold() {

    MetricRegistry metricRegistry = createHttpMetricRegistry(79);
    // By default threshold is 80%
    Config gerritConfig = new Config();
    gerritConfig.setInt("httpd", null, "maxThreads", 100);
    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HttpActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenNoActiveHttpWorkers() {

    MetricRegistry metricRegistry = createHttpMetricRegistry(0);

    Injector injector = testInjector(new TestModule(new Config(), metricRegistry));

    HttpActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  private MetricRegistry createHttpMetricRegistry(Integer value) {
    MetricRegistry metricRegistry = new MetricRegistry();

    metricRegistry.register(
        HttpActiveWorkersCheck.HTTP_WORKERS_METRIC_NAME,
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return value;
          }
        });
    return metricRegistry;
  }

  private HttpActiveWorkersCheck createCheck(Injector injector) {
    return createCheck(injector, DEFAULT_CONFIG);
  }

  private HttpActiveWorkersCheck createCheck(
      Injector injector, HealthCheckConfig healtchCheckConfig) {
    return new HttpActiveWorkersCheck(
        injector.getInstance(ListeningExecutorService.class),
        healtchCheckConfig,
        injector.getInstance(ThreadSettingsConfig.class),
        injector.getInstance(MetricRegistry.class));
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }

  private class TestModule extends AbstractModule {
    Config gerritConfig;
    MetricRegistry metricRegistry;

    public TestModule(Config gerritConfig, MetricRegistry metricRegistry) {
      this.gerritConfig = gerritConfig;
      this.metricRegistry = metricRegistry;
    }

    @Override
    protected void configure() {
      bind(Config.class).annotatedWith(GerritServerConfig.class).toInstance(gerritConfig);
      bind(ThreadSettingsConfig.class);
      bind(MetricRegistry.class).toInstance(metricRegistry);
    }
  }
}
