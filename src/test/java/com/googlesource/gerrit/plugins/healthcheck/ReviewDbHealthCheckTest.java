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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig.DEFAULT_CONFIG;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.lifecycle.LifecycleManager;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.testutil.DisabledReviewDb;
import com.google.gerrit.testutil.InMemoryDatabase;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.ReviewDbHealthCheck;
import org.junit.Before;
import org.junit.Test;

public class ReviewDbHealthCheckTest {
  private SchemaFactory<ReviewDb> workingReviewDbFactory;

  @Inject private ListeningExecutorService executor;

  @Before
  public void setUp() throws Exception {
    Guice.createInjector(new HealthCheckModule()).injectMembers(this);
    workingReviewDbFactory = InMemoryDatabase.newDatabase(new LifecycleManager()).create();
  }

  @Test
  public void shouldBeHealthyWhenReviewDbIsWorking() {
    ReviewDbHealthCheck reviewDbCheck =
        new ReviewDbHealthCheck(executor, DEFAULT_CONFIG, workingReviewDbFactory);
    assertThat(reviewDbCheck.run().result).isEqualTo(HealthCheck.Result.PASSED);
  }

  @Test
  public void shouldBeUnhealthyWhenReviewDbIsFailing() {
    ReviewDbHealthCheck reviewDbCheck =
        new ReviewDbHealthCheck(executor, DEFAULT_CONFIG, getFailingReviewDbProvider());
    assertThat(reviewDbCheck.run().result).isEqualTo(HealthCheck.Result.FAILED);
  }

  private SchemaFactory<ReviewDb> getFailingReviewDbProvider() {
    return new SchemaFactory<ReviewDb>() {
      @Override
      public ReviewDb open() {
        return new DisabledReviewDb();
      }
    };
  }
}
