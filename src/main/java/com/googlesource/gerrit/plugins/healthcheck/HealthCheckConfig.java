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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKEDTHREADS;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.INDEXWRITABLE;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.QUERYCHANGES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.config.GerritIsReplica;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
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
  private static final int ACTIVE_WORKERS_THRESHOLD_DEFAULT = 80;
  private static final String USERNAME_DEFAULT = "healthcheck";
  private static final String PASSWORD_DEFAULT = "";
  private static final String FAIL_FILE_FLAG_DEFAULT = "data/healthcheck/fail";
  private static final boolean HEALTH_CHECK_ENABLED_DEFAULT = true;
  private final AllProjectsName allProjectsName;
  private final AllUsersName allUsersName;

  private final Config config;
  private final boolean isReplica;

  private static final Set<String> HEALTH_CHECK_DISABLED_FOR_REPLICAS =
      Collections.singleton(QUERYCHANGES);

  @Inject
  public HealthCheckConfig(
      PluginConfigFactory configFactory,
      @PluginName String pluginName,
      AllProjectsName allProjectsName,
      AllUsersName allUsersName,
      @GerritIsReplica boolean isReplica) {
    config = configFactory.getGlobalPluginConfig(pluginName);
    this.allProjectsName = allProjectsName;
    this.allUsersName = allUsersName;
    this.isReplica = isReplica;
  }

  public String toText() {
    return config.toText();
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
    isReplica = false;
  }

  @VisibleForTesting
  void fromText(String configText) throws ConfigInvalidException {
    config.fromText(configText);
  }

  @VisibleForTesting
  void setString(
      final String section, final String subsection, final String name, final String value) {
    config.setString(section, subsection, name, value);
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

  public int getActiveWorkersThreshold(String healthCheckName) {
    int defaultThreshold =
        healthCheckName == null
            ? ACTIVE_WORKERS_THRESHOLD_DEFAULT
            : getActiveWorkersThreshold(null);
    return config.getInt(HEALTHCHECK, healthCheckName, "threshold", defaultThreshold);
  }

  public Set<Project.NameKey> getJGITRepositories(String healthCheckName) {
    Set<Project.NameKey> repos =
        Stream.of(config.getStringList(HEALTHCHECK, healthCheckName, "project"))
            .map(Project::nameKey)
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

  public String getFailFileFlagPath() {
    return getStringWithFallback("failFileFlagPath", null, FAIL_FILE_FLAG_DEFAULT);
  }

  public boolean healthCheckEnabled(String healthCheckName) {
    if (isReplica && HEALTH_CHECK_DISABLED_FOR_REPLICAS.contains(healthCheckName)) {
      return false;
    }
    return config.getBoolean(
        HEALTHCHECK, checkNotNull(healthCheckName), "enabled", HEALTH_CHECK_ENABLED_DEFAULT);
  }

  public String[] getListOfBlockedThreadsThresholds() {
    return config.getStringList(HEALTHCHECK, BLOCKEDTHREADS, "threshold");
  }

  public Project.NameKey getIndexWritableProjectName() {
    return Project.NameKey.parse(config.getString(HEALTHCHECK, INDEXWRITABLE, "projectName"));
  }

  public Change.Id getIndexWritableChangeId() {
    // TODO: What default do we use?
    return Change.id(config.getInt(HEALTHCHECK, INDEXWRITABLE, "changeId", 0));
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
