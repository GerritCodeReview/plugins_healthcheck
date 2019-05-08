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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
public class HealthCheckConfig {
  public static final String HEALTHCHECK = "healthcheck";
  public static final HealthCheckConfig DEFAULT_CONFIG = new HealthCheckConfig(null);
  private static final long HEALTHCHECK_TIMEOUT_DEFAULT = 500L;
  private static final String QUERY_DEFAULT = "status:open";
  private static final int LIMIT_DEFAULT = 10;
  private static final String USERNAME_DEFAULT = "healthcheck";
  private static final String PASSWORD_DEFAULT = "";
  private static final boolean HEALTH_CHECK_ENABLED_DEFAULT = true;
  private final AllProjectsName allProjectsName;
  private final AllUsersName allUsersName;

  private final Config config;

  @Inject
  public HealthCheckConfig(
      PluginConfigFactory configFactory,
      @PluginName String pluginName,
      AllProjectsName allProjectsName,
      AllUsersName allUsersName) {
    config = configFactory.getGlobalPluginConfig(pluginName);
    this.allProjectsName = allProjectsName;
    this.allUsersName = allUsersName;
  }

  @VisibleForTesting
  HealthCheckConfig(String configText) {
    config = new Config();
    if (!Strings.isNullOrEmpty(configText)) {
      try {
        config.fromText(configText);
      } catch (ConfigInvalidException e) {
        throw new IllegalArgumentException("Invalid configuration " + configText, e);
      }
    }
    allProjectsName = new AllProjectsName("All-Projects");
    allUsersName = new AllUsersName("All-Users");
  }

  @VisibleForTesting
  void fromText(String configText) throws ConfigInvalidException {
    config.fromText(configText);
  }

  public long getTimeout() {
    return getTimeout(null);
  }

  public long getTimeout(String healthCheckName) {
    long defaultTimeout = healthCheckName == null ? HEALTHCHECK_TIMEOUT_DEFAULT : getTimeout(null);
    return config.getTimeUnit(
        HEALTHCHECK, healthCheckName, "timeout", defaultTimeout, TimeUnit.MILLISECONDS);
  }

  public String getQuery(String healthCheckName) {
    return getStringWithFallback("query", healthCheckName, QUERY_DEFAULT);
  }

  public int getLimit(String healthCheckName) {
    int defaultLimit = healthCheckName == null ? LIMIT_DEFAULT : getLimit(null);
    return config.getInt(HEALTHCHECK, healthCheckName, "limit", defaultLimit);
  }

  public Set<Project.NameKey> getJGITRepositories(String healthCheckName) {
    Set<Project.NameKey> repos =
        Stream.of(config.getStringList(HEALTHCHECK, healthCheckName, "project"))
            .map(Project.NameKey::new)
            .collect(Collectors.toSet());
    repos.add(allProjectsName);
    repos.add(allUsersName);
    return repos;
  }

  public String getUsername(String healthCheckName) {
    return getStringWithFallback("username", healthCheckName, USERNAME_DEFAULT);
  }

  public String getPassword(String healthCheckName) {
    return getStringWithFallback("password", healthCheckName, PASSWORD_DEFAULT);
  }

  public boolean healthCheckEnabled(String healthCheckName) {
    return config.getBoolean(
        HEALTHCHECK, checkNotNull(healthCheckName), "enabled", HEALTH_CHECK_ENABLED_DEFAULT);
  }

  private String getStringWithFallback(
      String parameter, String healthCheckName, String defaultValue) {
    String fallbackDefault =
        healthCheckName == null
            ? defaultValue
            : getStringWithFallback(parameter, null, defaultValue);
    return MoreObjects.firstNonNull(
        config.getString(HEALTHCHECK, healthCheckName, parameter), fallbackDefault);
  }
}