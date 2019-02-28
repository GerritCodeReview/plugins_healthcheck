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
  protected StatusSummary latestStatus;
  protected boolean isEnabled;

  protected AbstractHealthCheck(
      ListeningExecutorService executor, HealthCheckConfig config, String name) {
    this.executor = executor;
    this.name = name;
    this.timeout = config.getTimeout(name);
    this.isEnabled = config.healthCheckEnabled(name);
    this.latestStatus = StatusSummary.INITIAL_STATUS;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public StatusSummary run() {
    final long ts = System.currentTimeMillis();
    ListenableFuture<StatusSummary> resultFuture =
        executor.submit(
            () -> {
              Result healthy;
              try {
                if (isEnabled) healthy = doCheck();
                else healthy = Result.DISABLED;
              } catch (Exception e) {
                log.warn("Check {} failed", name, e);
                healthy = Result.FAILED;
              }
              return new StatusSummary(healthy, ts, System.currentTimeMillis() - ts);
            });
    try {
      latestStatus = resultFuture.get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      log.warn("Check {} timed out", name, e);
      latestStatus = new StatusSummary(Result.TIMEOUT, ts, System.currentTimeMillis() - ts);
    } catch (InterruptedException | ExecutionException e) {
      log.warn("Check {} failed while waiting for its future result", name, e);
      latestStatus = new StatusSummary(Result.FAILED, ts, System.currentTimeMillis() - ts);
    }
    return latestStatus;
  }

  protected abstract Result doCheck() throws Exception;

  @Override
  public StatusSummary getLatestStatus() {
    return latestStatus;
  }
}