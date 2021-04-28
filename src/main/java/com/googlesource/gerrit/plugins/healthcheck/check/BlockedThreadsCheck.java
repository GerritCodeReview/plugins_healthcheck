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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BlockedThreadsCheck extends AbstractHealthCheck {
  public static Module SUB_CHECKS =
      new FactoryModule() {
        @Override
        protected void configure() {
          factory(BlockedThreadsSubCheck.Factory.class);
        }
      };

  private final ThreadMXBean threads;
  private final Supplier<List<Collector>> collectorsSupplier;

  @Inject
  public BlockedThreadsCheck(
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      ThreadBeanProvider threadBeanProvider,
      Provider<BlockedThreadsConfigurator> checksConfig) {
    super(executor, healthCheckConfig, BLOCKEDTHREADS, healthCheckMetricsFactory);
    this.threads = threadBeanProvider.get();
    this.collectorsSupplier = Suppliers.memoize(() -> checksConfig.get().collectors());
  }

  @Override
  protected Result doCheck() throws Exception {
    List<Collector> collectors = collectorsSupplier.get();
    dumpAllThreads().forEach(info -> collectors.forEach(c -> c.collect(info)));
    return collectors.stream()
        .map(Collector::check)
        .filter(r -> Result.FAILED == r)
        .findAny()
        .orElse(Result.PASSED);
  }

  private Stream<ThreadInfo> dumpAllThreads() {
    // getting all thread ids and translating it into thread infos is noticeably faster then call to
    // ThreadMXBean.dumpAllThreads as it doesn't calculate StackTrace. Note that some threads could
    // be already finished (between call to get all ids and translate them to ThreadInfo objects
    // hence they have to be filtered out).
    return Arrays.stream(threads.getThreadInfo(threads.getAllThreadIds(), 0))
        .filter(Objects::nonNull);
  }

  @VisibleForTesting
  public static class ThreadBeanProvider {
    public ThreadMXBean get() {
      return ManagementFactory.getThreadMXBean();
    }
  }

  static class Collector {
    protected final Integer threshold;

    protected int blocked;
    protected int total;

    Collector(Integer threshold) {
      this.threshold = threshold;
    }

    void collect(ThreadInfo info) {
      total += 1;
      if (Thread.State.BLOCKED == info.getThreadState()) {
        blocked += 1;
      }
    }

    Result check() {
      return total == 0 || (blocked * 100L) / total <= threshold ? Result.PASSED : Result.FAILED;
    }
  }

  interface CollectorProvider<T extends Collector> extends Provider<T> {}
}
