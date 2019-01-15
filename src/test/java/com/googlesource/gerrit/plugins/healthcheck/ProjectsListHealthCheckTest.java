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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.project.ListProjects;
import com.googlesource.gerrit.plugins.healthcheck.check.ProjectsListHealthCheck;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.Test;

public class ProjectsListHealthCheckTest {

  @Test
  public void shouldBeHealthyWhenListProjectsWorks() {
    ProjectsListHealthCheck jGitHealthCheck = new ProjectsListHealthCheck(getWorkingProjectList());
    assertThat(jGitHealthCheck.run().healthy).isTrue();
  }

  @Test
  public void shouldBeUnhealthyWhenListProjectsIsFailing() {
    ProjectsListHealthCheck jGitHealthCheck = new ProjectsListHealthCheck(getFailingProjectList());
    assertThat(jGitHealthCheck.run().healthy).isFalse();
  }

  private ListProjects getFailingProjectList() {
    return new ListProjects(null, null, null, null, null, null, null, null) {
      @Override
      public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
        throw new IllegalArgumentException("Unable to return project list");
      }
    };
  }

  private ListProjects getWorkingProjectList() {
    return new ListProjects(null, null, null, null, null, null, null, null) {
      @Override
      public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
        SortedMap<String, ProjectInfo> projects = new TreeMap<>();
        projects.put("testproject", new ProjectInfo());
        return projects;
      }
    };
  }
}
