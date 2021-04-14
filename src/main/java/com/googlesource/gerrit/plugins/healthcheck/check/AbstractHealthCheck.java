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
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
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
  private final MetricMaker metricMaker;

  private final Counter0 failureCounterMetric;
  private final Timer0 latencyMetric;

  protected AbstractHealthCheck(
      ListeningExecutorService executor, HealthCheckConfig config, String name, MetricMaker metricMaker) {
    this.executor = executor;
    this.name = name;
    this.timeout = config.getTimeout(name);
    this.config = config;
    this.latestStatus = StatusSummary.INITIAL_STATUS;
    this.metricMaker = metricMaker;

    this.failureCounterMetric = metricMaker.newCounter(
            String.format("%s/failure", name),
            new Description(String.format("%s healthcheck failures count", name))
                    .setCumulative()
                    .setRate()
                    .setUnit("failures"));
    this.latencyMetric = metricMaker.newTimer(String.format("%s/latest_latency", name),new Description(String.format("%s health check latency execution (ms)", name)).setCumulative().setUnit(Description.Units.MILLISECONDS));
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
              return new StatusSummary(
                  healthy, ts, System.currentTimeMillis() - ts, Collections.emptyMap());
            });
    try {
      checkStatusSummary = resultFuture.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Check {} timed out", name, e);
      checkStatusSummary =
          new StatusSummary(
              Result.TIMEOUT, ts, System.currentTimeMillis() - ts, Collections.emptyMap());
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Check {} failed while waiting for its future result", name, e);
      checkStatusSummary =
          new StatusSummary(
              Result.FAILED, ts, System.currentTimeMillis() - ts, Collections.emptyMap());
    }
    publishMetrics();
    latestStatus = checkStatusSummary.shallowCopy();
    return checkStatusSummary;
  }

  private void publishMetrics() {
    if (!latestStatus.result.equals(Result.PASSED) && !latestStatus.result.equals(Result.DISABLED)) {
      getFailureCounterMetric().increment();
    }
    getLatencyMetric().record(latestStatus.elapsed, TimeUnit.MILLISECONDS);
  }

  protected abstract Result doCheck() throws Exception;

  @Override
  public StatusSummary getLatestStatus() {
    return latestStatus;
  }

  @Override
  public Counter0 getFailureCounterMetric() { return this.failureCounterMetric; }

  @Override
  public Timer0 getLatencyMetric() { return this.latencyMetric; }
}
