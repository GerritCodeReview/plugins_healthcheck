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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
public class HealthCheckConfig {
  public static final String HEALTHCHECK = "healthcheck";
  private final Config config;
  private static final long HEALTHCHECK_TIMEOUT_DEFAULT = 500L;
  public static final HealthCheckConfig DEFAULT_CONFIG = new HealthCheckConfig(null);

  @Inject
  public HealthCheckConfig(PluginConfigFactory configFactory, @PluginName String pluginName) {
    config = configFactory.getGlobalPluginConfig(pluginName);
  }

  @VisibleForTesting
  public HealthCheckConfig(String configText) {
    config = new Config();
    if (!Strings.isNullOrEmpty(configText)) {
      try {
        config.fromText(configText);
      } catch (ConfigInvalidException e) {
        throw new IllegalArgumentException("Invalid configuration " + configText, e);
      }
    }
  }

  public long getTimeout() {
    return getTimeout(null);
  }

  public long getTimeout(String healthCheckName) {
    long defaultTimeout = healthCheckName == null ? HEALTHCHECK_TIMEOUT_DEFAULT : getTimeout(null);
    return config.getTimeUnit(
        HEALTHCHECK, healthCheckName, "timeout", defaultTimeout, TimeUnit.MILLISECONDS);
  }
}
