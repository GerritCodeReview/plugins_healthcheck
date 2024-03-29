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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Singleton
public class BlockedThreadsCheck extends AbstractHealthCheck {
  public static Module SUB_CHECKS =
      new FactoryModule() {
        @Override
        protected void configure() {
          factory(BlockedThreadsSubCheck.Factory.class);
        }
      };

  private final ThreadMXBean threads;
  private final BlockedThreadsConfigurator collectorsSupplier;

  @Inject
  public BlockedThreadsCheck(
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      MetricMaker metricMaker,
      ThreadBeanProvider threadBeanProvider,
      BlockedThreadsConfigurator checksConfig) {
    super(executor, healthCheckConfig, BLOCKEDTHREADS, metricMaker);
    this.threads = threadBeanProvider.get();
    this.collectorsSupplier = checksConfig;
  }

  @Override
  protected Result doCheck() throws Exception {
    List<Collector> collectors = collectorsSupplier.createCollectors();
    dumpAllThreads().forEach(info -> collectors.forEach(c -> c.collect(info)));

    // call check on all sub-checks so that metrics are populated
    collectors.forEach(Collector::check);

    // report unhealthy instance if any of sub-checks failed
    return collectors.stream()
        .map(Collector::result)
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
    protected Result result;

    Collector(Integer threshold) {
      this.threshold = threshold;
    }

    void collect(ThreadInfo info) {
      total += 1;
      if (Thread.State.BLOCKED == info.getThreadState()) {
        blocked += 1;
      }
    }

    void check() {
      result = blocked * 100 <= threshold * total ? Result.PASSED : Result.FAILED;
    }

    Result result() {
      return result;
    }
  }

  interface CollectorProvider<T extends Collector> extends Provider<T> {}
}
