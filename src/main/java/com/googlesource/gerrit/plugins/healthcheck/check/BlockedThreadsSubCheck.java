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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKEDTHREADS;

import com.google.common.base.Strings;
import com.google.gerrit.metrics.Counter0;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import java.lang.management.ThreadInfo;
import java.util.function.Supplier;

class BlockedThreadsSubCheck
    implements BlockedThreadsCheck.CollectorProvider<BlockedThreadsSubCheck.SubCheckCollector> {
  interface Factory {
    BlockedThreadsSubCheck create(String prefix, Integer threshold);
  }

  static class SubCheckCollector extends BlockedThreadsCheck.Collector {
    private final String prefix;
    private final Counter0 failureCounterMetric;

    SubCheckCollector(String prefix, Integer threshold, Counter0 failureCounterMetric) {
      super(threshold);
      this.prefix = prefix;
      this.failureCounterMetric = failureCounterMetric;
    }

    @Override
    void collect(ThreadInfo info) {
      String threadName = info.getThreadName();
      if (!Strings.isNullOrEmpty(threadName) && threadName.startsWith(prefix)) {
        total += 1;
        if (Thread.State.BLOCKED == info.getThreadState()) {
          blocked += 1;
        }
      }
    }

    @Override
    void check() {
      super.check();
      if (Result.FAILED == result) {
        failureCounterMetric.increment();
      }
    }
  }

  private final Supplier<SubCheckCollector> collector;

  @Inject
  BlockedThreadsSubCheck(
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      @Assisted String prefix,
      @Assisted Integer threshold) {
    HealthCheckMetrics healthCheckMetrics =
        healthCheckMetricsFactory.create(
            String.format("%s-%s", BLOCKEDTHREADS, prefix.toLowerCase()));
    Counter0 failureCounterMetric = healthCheckMetrics.getFailureCounterMetric();
    this.collector = () -> new SubCheckCollector(prefix, threshold, failureCounterMetric);
  }

  @Override
  public SubCheckCollector get() {
    return collector.get();
  }
}
