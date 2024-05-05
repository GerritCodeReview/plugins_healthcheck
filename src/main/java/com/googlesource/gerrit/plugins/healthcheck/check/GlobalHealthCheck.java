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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.GLOBAL;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class GlobalHealthCheck extends AbstractHealthCheck {

  private final DynamicSet<HealthCheck> healthChecks;

  @Inject
  public GlobalHealthCheck(
      DynamicSet<HealthCheck> healthChecks,
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      MetricMaker metricMaker) {
    super(executor, healthCheckConfig, GLOBAL, metricMaker);
    this.healthChecks = healthChecks;
  }

  @Override
  public HealthCheck.StatusSummary run() {
    Iterable<HealthCheck> iterable = () -> healthChecks.iterator();
    long ts = System.currentTimeMillis();
    Map<String, Object> checkToResults =
        StreamSupport.stream(iterable.spliterator(), true)
            .map(check -> Arrays.asList(check.name(), check.run()))
            .collect(Collectors.toMap(k -> (String) k.get(0), v -> v.get(1)));
    long elapsed = System.currentTimeMillis() - ts;
    StatusSummary globalStatus =
        new HealthCheck.StatusSummary(
            hasAnyFailureOnResults(checkToResults) ? Result.FAILED : Result.PASSED,
            ts,
            elapsed,
            checkToResults);
    if (globalStatus.isFailure()) {
      failureCounterMetric.increment();
    }
    latencyMetric.record(elapsed, TimeUnit.MILLISECONDS);
    return globalStatus;
  }

  @Override
  protected Result doCheck() {
    return run().result;
  }

  public static boolean hasAnyFailureOnResults(Map<String, Object> results) {
    return results.values().stream()
        .anyMatch(
            res ->
                res instanceof StatusSummary
                    && ((StatusSummary) res).isFailure());
  }
}
