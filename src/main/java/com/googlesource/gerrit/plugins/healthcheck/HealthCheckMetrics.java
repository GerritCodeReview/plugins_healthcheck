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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class HealthCheckMetrics implements LifecycleListener {

  private final DynamicSet<HealthCheck> healthChecks;
  private final MetricMaker metricMaker;
  private final Set<RegistrationHandle> registeredMetrics;
  private final Set<Runnable> triggers;

  @Inject
  public HealthCheckMetrics(DynamicSet<HealthCheck> healthChecks, MetricMaker metricMaker) {
    this.healthChecks = healthChecks;
    this.metricMaker = metricMaker;
    this.registeredMetrics = Collections.synchronizedSet(new HashSet<>());
    this.triggers = Collections.synchronizedSet(new HashSet<>());
  }

  @Override
  public void start() {

    for (HealthCheck healthCheck : healthChecks) {
      String name = healthCheck.name();

      Counter0 failureMetric =
          metricMaker.newCounter(
              String.format("%s/failure", name),
              new Description(String.format("%s healthcheck failures count", name))
                  .setCumulative()
                  .setRate()
                  .setUnit("failures"));

      CallbackMetric0<Long> latencyMetric =
          metricMaker.newCallbackMetric(
              String.format("%s/latest_measured_latency_ms", name),
              Long.class,
              new Description(String.format("%s health check latency execution (ms)", name))
                  .setGauge()
                  .setUnit(Description.Units.MILLISECONDS));

      Runnable metricCallBack =
          () -> {
            HealthCheck.StatusSummary status = healthCheck.getLatestStatus();
            latencyMetric.set(healthCheck.getLatestStatus().elapsed);
            if (status.isFailure()) {
              failureMetric.increment();
            }
          };

      registeredMetrics.add(failureMetric);
      registeredMetrics.add(metricMaker.newTrigger(latencyMetric, metricCallBack));
      triggers.add(metricCallBack);
    }
  }

  @Override
  public void stop() {
    for (RegistrationHandle handle : registeredMetrics) {
      handle.remove();
    }
  }

  @VisibleForTesting
  public void triggerAll() {
    triggers.forEach(Runnable::run);
  }
}
