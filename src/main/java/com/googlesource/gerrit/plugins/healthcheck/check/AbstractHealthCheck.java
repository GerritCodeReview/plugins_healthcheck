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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHealthCheck implements HealthCheck {
  private static final Logger log = LoggerFactory.getLogger(AbstractHealthCheck.class);
  private final long timeout;
  private final String name;
  private final ListeningExecutorService executor;
  protected volatile StatusSummary latestStatus;
  protected HealthCheckConfig config;

  protected final Counter0 failureCounterMetric;
  protected final Timer0 latencyMetric;

  protected AbstractHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      String name,
      MetricMaker metricMaker) {
    this.executor = executor;
    this.name = name;
    this.timeout = config.getTimeout(name);
    this.config = config;
    this.latestStatus = StatusSummary.INITIAL_STATUS;

    HealthCheckMetrics healthCheckMetrics = new HealthCheckMetrics(metricMaker, name);
    this.failureCounterMetric = healthCheckMetrics.getFailureCounterMetric();
    this.latencyMetric = healthCheckMetrics.getLatencyMetric();
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public StatusSummary run() {
    StatusSummary checkStatusSummary;
    boolean enabled = config.healthCheckEnabled(name);
    final long ts = System.currentTimeMillis();
    ListenableFuture<StatusSummary> resultFuture =
        executor.submit(
            () -> {
              Result healthy;
              try {
                healthy = enabled ? doCheck() : Result.DISABLED;
              } catch (Exception e) {
                log.warn("Check {} failed", name, e);
                healthy = Result.FAILED;
              }
              Long elapsed = System.currentTimeMillis() - ts;
              StatusSummary statusSummary =
                  new StatusSummary(healthy, ts, elapsed, Collections.emptyMap());
              if (statusSummary.isFailure()) {
                failureCounterMetric.increment();
              }
              latencyMetric.record(elapsed, TimeUnit.MILLISECONDS);
              return statusSummary;
            });
    try {
      checkStatusSummary = resultFuture.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Check {} timed out", name, e);
      Long elapsed = System.currentTimeMillis() - ts;
      checkStatusSummary = new StatusSummary(Result.TIMEOUT, ts, elapsed, Collections.emptyMap());
      failureCounterMetric.increment();
      latencyMetric.record(elapsed, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Check {} failed while waiting for its future result", name, e);
      Long elapsed = System.currentTimeMillis() - ts;
      checkStatusSummary = new StatusSummary(Result.FAILED, ts, elapsed, Collections.emptyMap());
      failureCounterMetric.increment();
      latencyMetric.record(elapsed, TimeUnit.MILLISECONDS);
    }
    return checkStatusSummary;
  }

  protected abstract Result doCheck() throws Exception;
}
