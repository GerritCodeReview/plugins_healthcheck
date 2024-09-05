// Copyright (C) 2020 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.HTTPACTIVEWORKERS;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;

public class HttpActiveWorkersCheck extends AbstractWorkersHealthCheck {
  public static final String HTTP_WORKERS_METRIC_NAME =
      "http/server/jetty/threadpool/active_threads";

  @Inject
  public HttpActiveWorkersCheck(
      ListeningExecutorService executor,
      HealthCheckConfig healthCheckConfig,
      ThreadSettingsConfig threadSettingsConfig,
      MetricRegistry metricRegistry) {
    super(
        executor,
        healthCheckConfig,
        metricRegistry,
        HTTPACTIVEWORKERS,
        HTTP_WORKERS_METRIC_NAME,
        threadSettingsConfig.getHttpdMaxThreads());
  }
}
