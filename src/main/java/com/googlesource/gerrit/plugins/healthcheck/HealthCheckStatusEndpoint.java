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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class HealthCheckStatusEndpoint implements RestReadView<ConfigResource> {

  private final DynamicSet<HealthCheck> healthChecks;

  @Inject
  public HealthCheckStatusEndpoint(DynamicSet<HealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }

  @Override
  public Object apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    Iterable<HealthCheck> iterable = () -> healthChecks.iterator();
    return StreamSupport.stream(iterable.spliterator(), true)
        .map(check -> check.run())
        .collect(Collectors.toMap(r -> r.name, r -> r.result));
  }
}
