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
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
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
  private final MetricsHandler metricsHandler;

  protected AbstractHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      String name,
      MetricsHandler.Factory metricsHandler) {
    this.executor = executor;
    this.name = name;
    this.metricsHandler = metricsHandler.create(name);
    this.timeout = config.getTimeout(name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Status run() {
    final long ts = System.currentTimeMillis();
    Status status = null;
    ListenableFuture<Status> resultFuture =
        executor.submit(
            () -> {
              Result healthy;
              try {
                healthy = doCheck();
              } catch (Exception e) {
                log.warn("Check {} failed", name, e);
                healthy = Result.FAILED;
              }
              return new Status(healthy, ts, System.currentTimeMillis() - ts);
            });
    try {
      status = resultFuture.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Check {} timed out", name, e);
      status = new Status(Result.TIMEOUT, ts, System.currentTimeMillis() - ts);
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Check {} failed while waiting for its future result", name, e);
      status = new Status(Result.FAILED, ts, System.currentTimeMillis() - ts);
    } finally {
      metricsHandler.sendMetrics(status);
    }
    return status;
  }

  protected abstract Result doCheck() throws Exception;
}
