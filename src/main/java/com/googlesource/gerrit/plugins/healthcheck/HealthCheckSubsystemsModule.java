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

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.googlesource.gerrit.plugins.healthcheck.check.ActiveWorkersCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.AuthHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.ChangeIndexHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.DeadlockCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.IndexHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.JGitHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.ProjectsListHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.QueryChangesHealthCheck;

public class HealthCheckSubsystemsModule extends FactoryModule {

  @Override
  protected void configure() {
    bindChecker(JGitHealthCheck.class);
    bindChecker(ProjectsListHealthCheck.class);
    bindChecker(QueryChangesHealthCheck.class);
    bindChecker(AuthHealthCheck.class);
    bindChecker(ActiveWorkersCheck.class);
    bindChecker(DeadlockCheck.class);
    bindChecker(BlockedThreadsCheck.class);
    bindChecker(IndexHealthCheck.class);

    install(BlockedThreadsCheck.SUB_CHECKS);
    install(ChangeIndexHealthCheck.module());
  }

  private void bindChecker(Class<? extends HealthCheck> healthCheckClass) {
    DynamicSet.bind(binder(), HealthCheck.class).to(healthCheckClass);
  }
}
