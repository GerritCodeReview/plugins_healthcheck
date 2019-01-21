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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.REVIEWDB;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.reviewdb.client.CurrentSchemaVersion;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;

public class ReviewDbHealthCheck extends AbstractHealthCheck {
  private final SchemaFactory<ReviewDb> reviewDb;

  @Inject
  public ReviewDbHealthCheck(ListeningExecutorService executor, SchemaFactory<ReviewDb> reviewDb, MetricMaker metricMaker) {
    super(executor, REVIEWDB, metricMaker);
    this.reviewDb = reviewDb;
  }

  @Override
  protected Result doCheck() throws Exception {
    try (ReviewDb db = reviewDb.open()) {
      db.schemaVersion().get(new CurrentSchemaVersion.Key());
      return Result.PASSED;
    }
  }
}
