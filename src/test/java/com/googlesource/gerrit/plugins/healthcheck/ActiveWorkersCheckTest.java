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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.ACTIVEWORKERS;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.ActiveWorkersCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class ActiveWorkersCheckTest {
  @Inject HealthCheckMetricsFactory healthCheckMetricsFactory;

  @Test
  public void shouldPassCheckWhenNoMetric() {

    Injector injector = testInjector(new TestModule(new Config(), new MetricRegistry()));

    ActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenNoActiveWorkers() {

    MetricRegistry metricRegistry = createMetricRegistry(0L);

    Injector injector = testInjector(new TestModule(new Config(), metricRegistry));

    ActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenActiveWorkersLessThanDefaultThreshold() {

    MetricRegistry metricRegistry = createMetricRegistry(79L);
    // By default threshold is 80%
    Config gerritConfig = new Config();
    gerritConfig.setInt("sshd", null, "threads", 100);
    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    ActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenActiveWorkersMoreThanDefaultThreshold() {

    MetricRegistry metricRegistry = createMetricRegistry(90L);
    // By default threshold is 80%
    Config gerritConfig = new Config();
    gerritConfig.setInt("sshd", null, "threads", 100);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    ActiveWorkersCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldFailCheckWhenActiveWorkersMoreThanThreshold() {

    MetricRegistry metricRegistry = createMetricRegistry(6L);

    Config gerritConfig = new Config();
    gerritConfig.setInt("sshd", null, "threads", 12);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HealthCheckConfig healthCheckConfig =
        new HealthCheckConfig("[healthcheck \"" + ACTIVEWORKERS + "\"]\n" + "  threshold = 50");
    ActiveWorkersCheck check = createCheck(injector, healthCheckConfig);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldPassCheckWhenActiveWorkersLessThanThreshold() {

    MetricRegistry metricRegistry = createMetricRegistry(5L);

    Config gerritConfig = new Config();
    gerritConfig.setInt("sshd", null, "threads", 12);

    Injector injector = testInjector(new TestModule(gerritConfig, metricRegistry));

    HealthCheckConfig healthCheckConfig =
        new HealthCheckConfig("[healthcheck \"" + ACTIVEWORKERS + "\"]\n" + "  threshold = 50");
    ActiveWorkersCheck check = createCheck(injector, healthCheckConfig);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }

  private MetricRegistry createMetricRegistry(Long value) {
    MetricRegistry metricRegistry = new MetricRegistry();
    metricRegistry.register(
        ActiveWorkersCheck.ACTIVE_WORKERS_METRIC_NAME,
        new Gauge<Long>() {
          @Override
          public Long getValue() {
            return value;
          }
        });
    return metricRegistry;
  }

  private ActiveWorkersCheck createCheck(Injector injector) {
    return createCheck(injector, DEFAULT_CONFIG);
  }

  private ActiveWorkersCheck createCheck(Injector injector, HealthCheckConfig healtchCheckConfig) {
    return new ActiveWorkersCheck(
        new Config(),
        injector.getInstance(ListeningExecutorService.class),
        healtchCheckConfig,
        injector.getInstance(ThreadSettingsConfig.class),
        injector.getInstance(MetricRegistry.class),
        healthCheckMetricsFactory);
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
