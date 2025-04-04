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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.PROJECTSLIST;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.util.SortedMap;

@Singleton
public class ProjectsListHealthCheck extends AbstractHealthCheck {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final int PROJECTS_LIST_LIMIT = 100;
  private final Provider<ListProjects> listProjectsProvider;
  private final OneOffRequestContext oneOffCtx;

  @Inject
  public ProjectsListHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      Provider<ListProjects> listProjectsProvider,
      OneOffRequestContext oneOffCtx,
      MetricMaker metricMaker) {
    super(executor, config, PROJECTSLIST, metricMaker);

    this.listProjectsProvider = listProjectsProvider;
    this.oneOffCtx = oneOffCtx;
  }

  @Override
  protected Result doCheck() {
    try (ManualRequestContext ctx = oneOffCtx.open()) {
      ListProjects listProjects = listProjectsProvider.get();
      listProjects.setStart(0);
      listProjects.setLimit(PROJECTS_LIST_LIMIT);
      listProjects.setShowDescription(true);
      listProjects.setMatchPrefix("All-");
      try {
        SortedMap<String, ProjectInfo> projects = listProjects.apply();
        if (projects != null && projects.size() > 0) {
          return Result.PASSED;
        }
        logger.atWarning().log(
            "Empty or null projects list: Gerrit should always have at least 1 project");
      } catch (Exception e) {
        logger.atWarning().withCause(e).log("Unable to list projects");
      }
      return Result.FAILED;
    }
  }
}
