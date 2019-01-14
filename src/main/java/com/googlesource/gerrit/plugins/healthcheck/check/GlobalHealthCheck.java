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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GlobalHealthCheck {

  private final DynamicSet<HealthCheck> healthChecks;

  @Inject
  public GlobalHealthCheck(DynamicSet<HealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }

  public Map<String, Boolean> run() {
    Iterable<HealthCheck> iterable = () -> healthChecks.iterator();
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(check -> check.run())
        .collect(Collectors.toMap(r -> r.name, r -> r.result));
  }
}
