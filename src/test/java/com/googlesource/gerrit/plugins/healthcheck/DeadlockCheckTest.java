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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.DeadlockCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class DeadlockCheckTest {

  HealthCheckMetricsFactory healthCheckMetricsFactory = new DummyHealthCheckMetricsFactory();

  @Test
  public void shouldPassCheckWhenNoMetric() {

    Injector injector = testInjector(new TestModule(new MetricRegistry()));

    DeadlockCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenNoDeadlock() {

    MetricRegistry metricRegistry = createMetricRegistry(0);

    Injector injector = testInjector(new TestModule(createMetricRegistry(0)));

    DeadlockCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenSingleDeadlock() {

    Injector injector = testInjector(new TestModule(createMetricRegistry(1)));

    DeadlockCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldFailCheckWhenMultipleDeadlocks() {

    Injector injector = testInjector(new TestModule(createMetricRegistry(5)));

    DeadlockCheck check = createCheck(injector);
    assertThat(check.run().result).isEqualTo(Result.FAILED);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }

  private MetricRegistry createMetricRegistry(Integer value) {
    MetricRegistry metricRegistry = new MetricRegistry();
    metricRegistry.register(
        DeadlockCheck.DEADLOCKED_THREADS_METRIC_NAME,
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return value;
          }
        });
    return metricRegistry;
  }

  private DeadlockCheck createCheck(Injector injector) {
    return new DeadlockCheck(
        injector.getInstance(ListeningExecutorService.class),
        DEFAULT_CONFIG,
        injector.getInstance(MetricRegistry.class),
        healthCheckMetricsFactory);
  }

  private class TestModule extends AbstractModule {
    Config gerritConfig;
    MetricRegistry metricRegistry;

    public TestModule(MetricRegistry metricRegistry) {
      this.gerritConfig = new Config();
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
