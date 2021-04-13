// Copyright (C) 2021 The Android Open Source Project
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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.check.GlobalHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.HashMap;
import org.junit.Test;

public class GlobalHealthCheckTest {

  @Inject private DynamicSet<HealthCheck> healthChecks;

  GlobalHealthCheck globalHealthCheck = new GlobalHealthCheck(healthChecks);

  @Test
  public void shouldFailIfOneCheckFails() {

    HashMap<String, Object> checks =
        new HashMap<String, Object>() {
          {
            put("failingCheck", new HealthCheck.StatusSummary(HealthCheck.Result.FAILED, 0L, 0L));
            put("passingCheck", new HealthCheck.StatusSummary(HealthCheck.Result.PASSED, 0L, 0L));
          }
        };
    assertThat(globalHealthCheck.getResultStatus(checks)).isEqualTo(HealthCheck.Result.FAILED);
  }

  @Test
  public void shouldPassIfAllChecksPass() {
    HashMap<String, Object> checks =
        new HashMap<String, Object>() {
          {
            put("passingCheck", new HealthCheck.StatusSummary(HealthCheck.Result.PASSED, 0L, 0L));
          }
        };
    assertThat(globalHealthCheck.getResultStatus(checks)).isEqualTo(HealthCheck.Result.PASSED);
  }
}
