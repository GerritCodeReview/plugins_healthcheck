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

import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.server.project.ListProjects;
import com.google.inject.Inject;
import java.util.SortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectsListHealthCheck extends AbstractHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(ProjectsListHealthCheck.class);
  private static final int PROJECTS_LIST_LIMIT = 100;
  private final ListProjects listProjects;

  @Inject
  public ProjectsListHealthCheck(ListProjects listProjects) {
    super(PROJECTSLIST);
    this.listProjects = listProjects;
  }

  @Override
  protected boolean doCheck() {
    listProjects.setStart(0);
    listProjects.setLimit(PROJECTS_LIST_LIMIT);
    listProjects.setShowDescription(true);
    try {
      SortedMap<String, ProjectInfo> projects = listProjects.apply();
      if (projects != null && projects.size() > 0) {
        return true;
      }
      log.warn("Empty or null projects list: Gerrit should always have at least 1 project");
      return false;
    } catch (Exception e) {
      log.warn("Unable to list projects", e);
      return false;
    }
  }
}
