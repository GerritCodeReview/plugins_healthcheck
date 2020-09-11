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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.util.Optional;

public abstract class AbstractWorkersCheck extends AbstractHealthCheck {

  private MetricRegistry metricRegistry;

  protected AbstractWorkersCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      MetricRegistry metricRegistry,
      String name) {

    super(executor, config, name);
    this.metricRegistry = metricRegistry;
  }

  public abstract String getMetricName();

  public abstract Integer getMaxPoolSize();

  public abstract Integer getThreshold();

  @Override
  protected Result doCheck() throws Exception {
    return Optional.ofNullable(metricRegistry.getGauges().get(getMetricName()))
        .map(
            metric -> {
              float currentThreadsPercentage = (getMetricValue(metric) * 100) / getMaxPoolSize();
              return (currentThreadsPercentage <= getThreshold()) ? Result.PASSED : Result.FAILED;
            })
        .orElse(Result.PASSED);
  }

  private Long getMetricValue(Gauge<?> metric) {
    Object value = metric.getValue();
    if (value instanceof Long) {
      return (Long) value;
    }

    if (value instanceof Integer) {
      return Long.valueOf((int) metric.getValue());
    }

    throw new IllegalArgumentException(
        String.format(
            "Workers metric value must be of type java.lang.Long or java.lang.Integer but was %s ",
            value.getClass().getName()));
  }
}
