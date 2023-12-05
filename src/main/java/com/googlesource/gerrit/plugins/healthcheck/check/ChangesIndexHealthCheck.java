// Copyright (C) 2023 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.CHANGES_INDEX;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.index.IndexType;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ChangesIndexHealthCheck extends AbstractHealthCheck {
  interface Checker {
    Result doCheck() throws Exception;
  }

  private static final Logger log = LoggerFactory.getLogger(QueryChangesHealthCheck.class);

  private final Checker checker;

  @Inject
  ChangesIndexHealthCheck(
      @GerritServerConfig Config cfg,
      ListeningExecutorService executor,
      HealthCheckConfig config,
      MetricMaker metricMaker,
      Provider<EnabledChangesIndexHealthCheck> enabled,
      Provider<DisabledChangesIndexHealthCheck> disabled) {
    super(executor, config, CHANGES_INDEX, metricMaker);
    this.checker = (isIndexTypeLucene(cfg) ? enabled : disabled).get();
  }

  @Override
  protected Result doCheck() throws Exception {
    return checker.doCheck();
  }

  private static boolean isIndexTypeLucene(Config cfg) {
    IndexType indexType = new IndexType(cfg.getString("index", null, "type"));
    boolean isLucene = indexType.isLucene();
    if (!isLucene) {
      log.warn(
          "Configured index type [{}] is not supported for index health check therefore it is disabled.",
          indexType);
    }
    return isLucene;
  }
}
