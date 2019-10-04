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
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.googlesource.gerrit.plugins.healthcheck.filter.HealthCheckStatusFilter;
import org.eclipse.jgit.lib.Config;
import plugins.healthcheck.src.main.java.com.googlesource.gerrit.plugins.healthcheck.filter.HealthCheckGsonProvider;

public class HttpModule extends ServletModule {
  private boolean isSlave;

  @Inject
  public HttpModule(@GerritServerConfig Config gerritConfig) {
    isSlave = gerritConfig.getBoolean("container", "slave", false);
  }

  @Override
  protected void configureServlets() {
    if (isSlave) {
      DynamicSet.bind(binder(), AllRequestFilter.class)
          .to(HealthCheckStatusFilter.class)
          .in(Scopes.SINGLETON);

      bind(Gson.class).toProvider(HealthCheckGsonProvider.class).in(Scopes.SINGLETON);
    }
  }
}
