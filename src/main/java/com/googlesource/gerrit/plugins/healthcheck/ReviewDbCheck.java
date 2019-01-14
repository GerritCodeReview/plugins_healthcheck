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

package com.googlesource.gerrit.plugins.healthcheck;

import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckNames.REVIEWDB;

import com.google.gerrit.reviewdb.client.CurrentSchemaVersion;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewDbCheck implements HealthCheck {
  private static final Logger log = LoggerFactory.getLogger(ReviewDbCheck.class);

  private final Provider<ReviewDb> reviewDb;

  @Inject
  public ReviewDbCheck(Provider<ReviewDb> reviewDb) {
    this.reviewDb = reviewDb;
  }

  @Override
  public HealthCheck.Result run() {
    return new HealthCheck.Result(REVIEWDB, doCheck());
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
