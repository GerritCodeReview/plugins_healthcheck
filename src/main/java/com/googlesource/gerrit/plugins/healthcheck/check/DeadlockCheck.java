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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.DEADLOCK;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.util.Optional;

public class DeadlockCheck extends AbstractHealthCheck {

  public static final String DEADLOCKED_THREADS_METRIC_NAME =
      "proc/jvm/thread/num_deadlocked_threads";

  private final MetricRegistry metricRegistry;
  private HealthCheckMetrics.Factory healthCheckMetricsFactory;

  @Inject
  public DeadlockCheck(
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      MetricRegistry metricRegistry,
      HealthCheckMetrics.Factory healthCheckMetricsFactory) {
    super(executor, healthCheckConfig, DEADLOCK, healthCheckMetricsFactory);
    this.metricRegistry = metricRegistry;
  }

  @Override
  protected Result doCheck() throws Exception {
    return Optional.ofNullable(metricRegistry.getGauges().get(DEADLOCKED_THREADS_METRIC_NAME))
        .map(
            metric -> {
              return (int) metric.getValue() == 0 ? Result.PASSED : Result.FAILED;
            })
        .orElse(Result.PASSED);
  }
}
