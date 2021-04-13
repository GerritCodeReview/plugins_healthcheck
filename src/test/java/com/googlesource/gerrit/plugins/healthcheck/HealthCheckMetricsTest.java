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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.StatusSummary;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class HealthCheckMetricsTest {

  @Inject private ListeningExecutorService executor;
  private TestMetrics testMetrics = new TestMetrics();

  @Before
  public void setUp() throws Exception {
    testMetrics.reset();
  }

  private void setWithStatusSummary(StatusSummary StatusSummary) {
    TestHealthCheck testHealthCheck = new TestHealthCheck(executor, "test", StatusSummary);

    Injector injector =
        testInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                DynamicSet.bind(binder(), HealthCheck.class).toInstance(testHealthCheck);
                bind(MetricMaker.class).toInstance(testMetrics);
                bind(LifecycleListener.class).to(HealthCheckMetrics.class);
              }
            });
    HealthCheckMetrics healthCheckMetrics = injector.getInstance(HealthCheckMetrics.class);

    healthCheckMetrics.start();
    testHealthCheck.run();
    healthCheckMetrics.triggerAll();
  }

  @Test
  public void shouldSendCounterWhenStatusSummaryFailed() {
    Long elapsed = 100L;
    setWithStatusSummary(new StatusSummary(Result.FAILED, 1L, elapsed, Collections.emptyMap()));

    assertThat(testMetrics.getFailures()).isEqualTo(1);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }

  @Test
  public void shouldSendCounterWhenStatusSummaryTimeout() {
    Long elapsed = 100L;
    setWithStatusSummary(new StatusSummary(Result.TIMEOUT, 1L, elapsed, Collections.emptyMap()));

    assertThat(testMetrics.getFailures()).isEqualTo(1);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }

  @Test
  public void shouldNOTSendCounterWhenStatusSummarySuccess() {
    Long elapsed = 100L;
    setWithStatusSummary(new StatusSummary(Result.PASSED, 1L, elapsed, Collections.emptyMap()));

    assertThat(testMetrics.failures).isEqualTo(0L);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }

  @Singleton
  private static class TestMetrics extends DisabledMetricMaker {
    private Long failures = 0L;
    private Long latency = 0L;

    public Long getFailures() {
      return failures;
    }

    public Long getLatency() {
      return latency;
    }

    public void reset() {
      failures = 0L;
      latency = 0L;
    }

    @Override
    public Counter0 newCounter(String name, Description desc) {
      return new Counter0() {
        @Override
        public void incrementBy(long value) {
          if (!name.startsWith("global/")) {
            failures += value;
          }
        }

        @Override
        public void remove() {}
      };
    }

    @Override
    public <V> CallbackMetric0<V> newCallbackMetric(
        String name, Class<V> valueClass, Description desc) {
      return new CallbackMetric0<V>() {
        @Override
        public void set(V value) {
          if (!name.startsWith("global/")) {
            latency = (Long) value;
          }
        }

        @Override
        public void remove() {}
      };
    }
  }

  private static class TestHealthCheck extends AbstractHealthCheck {

    protected TestHealthCheck(
        ListeningExecutorService executor, String name, StatusSummary returnStatusSummary) {
      super(executor, HealthCheckConfig.DEFAULT_CONFIG, name);
      this.latestStatus = returnStatusSummary;
    }

    @Override
    protected Result doCheck() {
      return latestStatus.result;
    }

    @Override
    public StatusSummary run() {
      return latestStatus;
    }
  }
}
