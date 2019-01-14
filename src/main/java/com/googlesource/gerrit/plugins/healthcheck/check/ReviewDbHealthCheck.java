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

import com.google.gerrit.reviewdb.client.CurrentSchemaVersion;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewDbHealthCheck implements HealthCheck {
  private static final Logger log = LoggerFactory.getLogger(ReviewDbHealthCheck.class);

  private final Provider<ReviewDb> reviewDb;

  @Inject
  public ReviewDbHealthCheck(Provider<ReviewDb> reviewDb) {
    this.reviewDb = reviewDb;
  }

  @Override
  public HealthCheck.Result run() {
    boolean healthy = false;
    long ts = System.currentTimeMillis();
    try {
      healthy = doCheck();
    } finally {
      return new HealthCheck.Result(healthy, ts, System.currentTimeMillis() - ts);
    }
  }

  @Override
  public String name() {
    return REVIEWDB;
  }

  private boolean doCheck() {
    try (ReviewDb db = reviewDb.get()) {
      db.schemaVersion().get(new CurrentSchemaVersion.Key());
      return true;
    } catch (Exception e) {
      log.warn("Unable to open ReviewDb", e);
      return false;
    }
  }
}
