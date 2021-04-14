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
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class HealthCheckMetrics {

  private final MetricMaker metricMaker;
  private final String name;

  @Inject
  public HealthCheckMetrics(MetricMaker metricMaker, @Assisted String name) {
    this.metricMaker = metricMaker;
    this.name = name;
  }

  public Counter0 getFailureCounterMetric() {
    Counter0 failureCounter =
        metricMaker.newCounter(
            String.format("%s/failure", name),
            new Description(String.format("%s healthcheck failures count", name))
                .setCumulative()
                .setRate()
                .setUnit("failures"));

    return failureCounter;
  }

  public Timer0 getLatencyMetric() {
    return metricMaker.newTimer(
        String.format("%s/latest_latency", name),
        new Description(String.format("%s health check latency execution (ms)", name))
            .setCumulative()
            .setUnit(Description.Units.MILLISECONDS));
  }
}
