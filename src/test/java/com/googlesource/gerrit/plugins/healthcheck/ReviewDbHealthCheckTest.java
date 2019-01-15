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

import com.google.gerrit.lifecycle.LifecycleManager;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.testing.DisabledReviewDb;
import com.google.gerrit.testing.InMemoryDatabase;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.healthcheck.check.ReviewDbHealthCheck;
import org.junit.Test;

public class ReviewDbHealthCheckTest {

  @Test
  public void shouldBeHealthyWhenReviewDbIsWorking() throws OrmException {
    ReviewDbHealthCheck reviewDbCheck = new ReviewDbHealthCheck(getWorkingReviewDbProvider());
    assertThat(reviewDbCheck.run().healthy).isTrue();
  }

  @Test
  public void shouldBeUnhealthyWhenReviewDbIsFailing() {
    ReviewDbHealthCheck reviewDbCheck = new ReviewDbHealthCheck(getFailingReviewDbProvider());
    assertThat(reviewDbCheck.run().healthy).isFalse();
  }

  private Provider<ReviewDb> getFailingReviewDbProvider() {
    return new Provider<ReviewDb>() {
      @Override
      public ReviewDb get() {
        return new DisabledReviewDb();
      }
    };
  }

  private Provider<ReviewDb> getWorkingReviewDbProvider() throws OrmException {
    InMemoryDatabase inMemoryDatabase =
        InMemoryDatabase.newDatabase(new LifecycleManager()).create();
    return new Provider<ReviewDb>() {
      @Override
      public ReviewDb get() {
        try {
          return inMemoryDatabase.open();
        } catch (OrmException e) {
          e.printStackTrace();
          return null;
        }
      }
    };
  }
}
