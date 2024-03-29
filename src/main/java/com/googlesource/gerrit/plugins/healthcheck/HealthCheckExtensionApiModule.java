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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import java.util.concurrent.Executors;

public class HealthCheckExtensionApiModule extends AbstractModule {
  public static final int CHECK_THREADS_DEFAULT = 10;

  @Override
  protected void configure() {
    bind(ListeningExecutorService.class)
        .toInstance(
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(CHECK_THREADS_DEFAULT)));
    DynamicSet.setOf(binder(), HealthCheck.class);
  }
}
