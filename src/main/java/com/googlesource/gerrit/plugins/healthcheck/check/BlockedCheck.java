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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKED;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockedCheck extends AbstractHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(BlockedCheck.class);
  private static final int DEFAULT_BLOCKED_THRESHOLD = 50;

  private final Integer threshold;
  private final ThreadMXBean threads;
  private final Supplier<Boolean> isSupported;

  @Inject
  public BlockedCheck(
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      ThreadBeanProvider threadBeanProvider) {
    super(executor, healthCheckConfig, BLOCKED, healthCheckMetricsFactory);
    this.threshold = healthCheckConfig.getHealthcheckThreshold(BLOCKED, DEFAULT_BLOCKED_THRESHOLD);
    this.threads = threadBeanProvider.get();
    this.isSupported = Suppliers.memoize(this::checkIfSupported);
  }

  @Override
  protected boolean isCheckEnabled(String name) {
    return super.isCheckEnabled(name) && isSupported.get();
  }

  @Override
  protected Result doCheck() throws Exception {
    return Optional.ofNullable(
            threads.dumpAllThreads(
                threads.isObjectMonitorUsageSupported(), threads.isSynchronizerUsageSupported()))
        .map(
            infos -> {
              long blocked =
                  Arrays.stream(infos)
                      .filter(t -> Thread.State.BLOCKED == t.getThreadState())
                      .count();
              return (blocked * 100L) / infos.length <= threshold ? Result.PASSED : Result.FAILED;
            })
        .orElse(Result.PASSED);
  }

  private boolean checkIfSupported() {
    if (!threads.isSynchronizerUsageSupported() && !threads.isObjectMonitorUsageSupported()) {
      log.warn(
          "Healthcheck '{}' is enabled in configuration but it is not supported by the underlying"
              + " Java",
          BLOCKED);
      return false;
    }

    return true;
  }

  @VisibleForTesting
  public static class ThreadBeanProvider {
    public ThreadMXBean get() {
      return ManagementFactory.getThreadMXBean();
    }
  }
}
