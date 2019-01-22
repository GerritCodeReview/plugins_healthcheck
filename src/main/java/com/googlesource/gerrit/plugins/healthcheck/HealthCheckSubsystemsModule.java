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
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.JGitHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.MetricsHandler;
import com.googlesource.gerrit.plugins.healthcheck.check.ProjectsListHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.ReviewDbHealthCheck;

public class HealthCheckSubsystemsModule extends AbstractModule {

  @Override
  protected void configure() {
    install(
        new FactoryModuleBuilder()
            .implement(MetricsHandler.class, MetricsHandler.class)
            .build(MetricsHandler.Factory.class));

    bindChecker(ReviewDbHealthCheck.class);
    bindChecker(JGitHealthCheck.class);
    bindChecker(ProjectsListHealthCheck.class);
  }

  private void bindChecker(Class<? extends HealthCheck> healthCheckClass) {
    DynamicSet.bind(binder(), HealthCheck.class).to(healthCheckClass);
  }
}
