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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class GlobalHealthCheck extends AbstractHealthCheck {

  private final DynamicSet<HealthCheck> healthChecks;

  public static class MemoizedStatusSummary implements Supplier<StatusSummary> {
    private final AtomicReference<StatusSummary> result = new AtomicReference<>();
    private final HealthCheck check;

    MemoizedStatusSummary(HealthCheck check) {
      this.check = check;
    }

    @Override
    public StatusSummary get() {
      if (result.get() == null) {
        result.set(check.run());
      }
      return result.get();
    }

    public StatusSummary getIfCompleted() {
      StatusSummary completedResult = result.get();
      return completedResult == null
          ? new StatusSummary(
              Result.NOT_RUN, System.currentTimeMillis(), 0L, Collections.emptyMap())
          : completedResult;
    }
  }

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
<<<<<<< PATCH SET (8fce7b Short-circuit on failed healthchecks)
    Map<String, MemoizedStatusSummary> checkToResults =
        StreamSupport.stream(iterable.spliterator(), false)
            .collect(Collectors.toMap(HealthCheck::name, MemoizedStatusSummary::new));
=======
    Map<String, Object> checkToResults =
        StreamSupport.stream(iterable.spliterator(), true)
            .collect(Collectors.toMap(HealthCheck::name, HealthCheck::run));
>>>>>>> BASE      (44cf06 Simplify construction of map of check results)
    long elapsed = System.currentTimeMillis() - ts;
    Result checkResult = hasAnyFailureOnResults(checkToResults) ? Result.FAILED : Result.PASSED;
    Map<String, Object> reportedResults =
        checkToResults.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getIfCompleted()));
    StatusSummary globalStatus =
        new HealthCheck.StatusSummary(checkResult, ts, elapsed, reportedResults);
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

<<<<<<< PATCH SET (8fce7b Short-circuit on failed healthchecks)
  public static boolean hasAnyFailureOnResults(Map<String, MemoizedStatusSummary> results) {
    return results.values().stream().parallel().anyMatch(res -> res.get().isFailure());
=======
  public static boolean hasAnyFailureOnResults(Map<String, Object> results) {
    return results.values().stream()
        .anyMatch(res -> res instanceof StatusSummary && ((StatusSummary) res).isFailure());
>>>>>>> BASE      (44cf06 Simplify construction of map of check results)
  }
}
