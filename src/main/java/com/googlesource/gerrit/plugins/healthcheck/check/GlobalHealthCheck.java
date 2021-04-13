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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class GlobalHealthCheck {

  private final DynamicSet<HealthCheck> healthChecks;
  private HealthCheck.StatusSummary globalStatusSummary;

  @Inject
  public GlobalHealthCheck(DynamicSet<HealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
    this.globalStatusSummary = HealthCheck.StatusSummary.INITIAL_STATUS;
  }

  public Map<String, Object> run() {
    Iterable<HealthCheck> iterable = () -> healthChecks.iterator();
    long ts = System.currentTimeMillis();
    Map<String, Object> checkToResults = StreamSupport.stream(iterable.spliterator(), false)
        .map(check -> Arrays.asList(check.name(), check.run()))
        .collect(Collectors.toMap(k -> (String) k.get(0), v -> v.get(1)));
    long elapsed = System.currentTimeMillis() - ts;
    globalStatusSummary = new HealthCheck.StatusSummary(getResultStatus(checkToResults), ts, elapsed);
    return checkToResults;
  }

  public HealthCheck.StatusSummary getGlobalStatusSummary() {return this.globalStatusSummary;}

  public HealthCheck.Result getResultStatus(Map<String, Object> result) {
    if (result.values().stream()
            .filter(
                    res ->
                            res instanceof HealthCheck.StatusSummary
                                    && ((HealthCheck.StatusSummary) res).isFailure())
            .findFirst()
            .isPresent()) {
      return HealthCheck.Result.FAILED;
    }
    return HealthCheck.Result.PASSED;
  }
}
