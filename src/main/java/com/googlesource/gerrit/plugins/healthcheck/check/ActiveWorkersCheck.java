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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.ACTIVEWORKERS;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import org.eclipse.jgit.lib.Config;

@Singleton
public class ActiveWorkersCheck extends AbstractWorkersHealthCheck {

  public static final String ACTIVE_WORKERS_METRIC_NAME =
      "queue/ssh_interactive_worker/active_threads";

  @Inject
  public ActiveWorkersCheck(
      @GerritServerConfig Config gerritConfig,
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      ThreadSettingsConfig threadSettingsConfig,
      MetricRegistry metricRegistry,
      HealthCheckMetrics.Factory healthCheckMetricsFactory) {

    super(
        executor,
        healthCheckConfig,
        metricRegistry,
        ACTIVEWORKERS,
        ACTIVE_WORKERS_METRIC_NAME,
        getInteractiveThreadsMaxPoolSize(threadSettingsConfig, gerritConfig),
        healthCheckMetricsFactory);
  }

  /**
   * This method is following logic from com.google.gerrit.sshd.CommandExecutorQueueProvider
   *
   * <p>We are not able to use "queue/ssh_interactive_worker/max_pool_size" metric because in
   * current implementation it's always returning Integer.MAX_VALUE.
   *
   * @return max number of allowed threads in interactive work queue
   */
  private static Integer getInteractiveThreadsMaxPoolSize(
      ThreadSettingsConfig threadSettingsConfig, Config gerritConfig) {
    int poolSize = threadSettingsConfig.getSshdThreads();
    int batchThreads =
        gerritConfig.getInt("sshd", "batchThreads", threadSettingsConfig.getSshdBatchTreads());
    if (batchThreads > poolSize) {
      poolSize += batchThreads;
    }
    return Math.max(1, poolSize - batchThreads);
  }
}
