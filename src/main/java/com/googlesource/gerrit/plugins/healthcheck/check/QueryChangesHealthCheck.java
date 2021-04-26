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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class QueryChangesHealthCheck extends AbstractHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(QueryChangesHealthCheck.class);
  private final Provider<QueryChanges> queryChangesProvider;
  private final int limit;
  private final OneOffRequestContext oneOffCtx;

  @Inject
  public QueryChangesHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      Provider<QueryChanges> queryChangesProvider,
      OneOffRequestContext oneOffCtx,
      HealthCheckMetrics.Factory healthCheckMetricsFactory) {
    super(executor, config, QUERYCHANGES, healthCheckMetricsFactory);
    this.queryChangesProvider = queryChangesProvider;
    this.config = config;
    this.limit = config.getLimit(QUERYCHANGES);

    this.oneOffCtx = oneOffCtx;
  }

  @Override
  protected Result doCheck() throws Exception {
    try (ManualRequestContext ctx = oneOffCtx.open()) {

      QueryChanges queryChanges = this.queryChangesProvider.get();
      queryChanges.setLimit(limit);
      queryChanges.addQuery(config.getQuery(QUERYCHANGES));
      queryChanges.setStart(0);

      List<?> changes = queryChanges.apply(null).value();
      if (changes == null) {
        log.warn("Cannot query changes: received a null list of results");
        return Result.FAILED;
      }

      if (changes.size() < limit) {
        log.warn(
            "Query changes did not return enough items: expected {} items but got only {}",
            limit,
            changes.size());
        return Result.FAILED;
      }

      return Result.PASSED;
    }
  }
}
