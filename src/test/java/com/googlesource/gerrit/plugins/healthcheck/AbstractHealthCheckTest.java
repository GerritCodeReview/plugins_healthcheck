// Copyright (C) 2021 The Android Open Source Project
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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class AbstractHealthCheckTest {

  TestMetricMaker testMetricMaker;

  @Before
  public void setUp() throws Exception {
    testMetricMaker = new TestMetricMaker();
  }

  @Test
  public void shouldPublishAllMetricsWhenFailing() {
    TestCheck testCheck = createFailingTestCheck();

    testCheck.run();

    assertThat(testMetricMaker.getFailureCount()).isEqualTo(TestMetricMaker.expectedFailureCount);
    assertThat(testMetricMaker.getLatency()).isEqualTo(TestMetricMaker.expectedLatency);
  }

  @Test
  public void shouldPublishAllMetricsWhenTimingOut() {
    TestCheck testCheck = createTimingOutTestCheck();

    testCheck.run();

    assertThat(testMetricMaker.getFailureCount()).isEqualTo(TestMetricMaker.expectedFailureCount);
    assertThat(testMetricMaker.getLatency()).isEqualTo(TestMetricMaker.expectedLatency);
  }

  @Test
  public void shouldPublishOnlyLatencyMetricsWhenPassing() {
    TestCheck testCheck = createPassingTestCheck();

    testCheck.run();

    assertThat(testMetricMaker.getFailureCount()).isEqualTo(0L);
    assertThat(testMetricMaker.getLatency()).isEqualTo(TestMetricMaker.expectedLatency);
  }

  @Test
  public void shouldPublishOnlyLatencyMetricsWhenDisabled() {
    TestCheck testCheck = createDisabledTestCheck();

    testCheck.run();

    assertThat(testMetricMaker.getFailureCount()).isEqualTo(0L);
    assertThat(testMetricMaker.getLatency()).isEqualTo(TestMetricMaker.expectedLatency);
  }

  public TestCheck createPassingTestCheck() {
    return createTestCheckWithStatus(HealthCheck.Result.PASSED);
  }

  public TestCheck createFailingTestCheck() {
    return createTestCheckWithStatus(HealthCheck.Result.FAILED);
  }

  public TestCheck createDisabledTestCheck() {
    return createTestCheckWithStatus(HealthCheck.Result.DISABLED);
  }

  public TestCheck createTimingOutTestCheck() {
    return createTestCheckWithStatus(HealthCheck.Result.TIMEOUT);
  }

  private TestCheck createTestCheckWithStatus(HealthCheck.Result result) {
    return new TestCheck(HealthCheckConfig.DEFAULT_CONFIG, "testCheck", testMetricMaker, result);
  }

  private static class TestCheck extends AbstractHealthCheck {
    private final Result finalResult;

    public TestCheck(
        HealthCheckConfig config, String name, MetricMaker metricMaker, Result finalResult) {
      super(
          MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10)),
          config,
          name,
          metricMaker);
      this.finalResult = finalResult;
    }

    @Override
    protected Result doCheck() throws Exception {
      return finalResult;
    }
  }

  private static class TestMetricMaker extends DisabledMetricMaker {
    private Long failureCount = 0L;
    private Long latency = 0L;
    public static Long expectedFailureCount = 1L;
    public static Long expectedLatency = 100L;

    @Override
    public Counter0 newCounter(String name, Description desc) {
      return new Counter0() {

        @Override
        public void incrementBy(long value) {
          failureCount += value;
        }

        @Override
        public void increment() {
          failureCount = expectedFailureCount;
        }

        @Override
        public void remove() {}
      };
    }

    @Override
    public Timer0 newTimer(String name, Description desc) {
      return new Timer0(name) {
        @Override
        protected void doRecord(long value, TimeUnit unit) {
          latency = expectedLatency;
        }

        @Override
        public void remove() {}
      };
    }

    public Long getLatency() {
      return latency;
    }

    public Long getFailureCount() {
      return failureCount;
    }
  }
}
