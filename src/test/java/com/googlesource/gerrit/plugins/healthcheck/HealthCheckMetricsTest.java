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

import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.Timer0;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class HealthCheckMetricsTest {

  @Test
  public void shouldReturnACounterWithCorrectValues() {
    String testMetricName = "testMetric";
    TestMetrics testMetrics = new TestMetrics();
    HealthCheckMetrics healthCheckMetrics = new HealthCheckMetrics(testMetrics, testMetricName);
    healthCheckMetrics.getFailureCounterMetric();
    assertThat(testMetrics.metricName).isEqualTo(testMetricName + "/failure");
    assertThat(testMetrics.metricDescription.toString()).isEqualTo(new Description(String.format("%s healthcheck failures count", testMetricName)).setCumulative()
            .setRate()
            .setUnit("failures").toString());
  }

  @Test
  public void shouldReturnATimerWithCorrectValues() {
    String testMetricName = "testMetric";
    TestMetrics testMetrics = new TestMetrics();
    HealthCheckMetrics healthCheckMetrics = new HealthCheckMetrics(testMetrics, testMetricName);
    healthCheckMetrics.getLatencyMetric();
    assertThat(testMetrics.metricName).isEqualTo(testMetricName + "/latest_latency");
    assertThat(testMetrics.metricDescription.toString()).isEqualTo(new Description(String.format("%s health check latency execution (ms)", testMetricName))
            .setCumulative()
            .setUnit(Description.Units.MILLISECONDS).toString());
  }


  private static class TestMetrics extends DisabledMetricMaker {
    String metricName;
    Description metricDescription;

    @Override
    public Counter0 newCounter(String name, Description desc) {
      metricName = name;
      metricDescription = desc;
      return new Counter0() {

        @Override
        public void incrementBy(long value) {
        }
        @Override
        public void remove() {}

      };
    }

    @Override
    public Timer0 newTimer(String name, Description desc) {
      metricName = name;
      metricDescription = desc;
      return new Timer0(name) {
        @Override
        protected void doRecord(long value, TimeUnit unit) {}

        @Override
        public void remove() {}
      };
    }

  }
}
