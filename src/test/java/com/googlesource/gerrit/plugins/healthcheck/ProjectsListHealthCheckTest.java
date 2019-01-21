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
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.ProjectsListHealthCheck;
import org.junit.Before;
import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;

import static com.google.common.truth.Truth.assertThat;

public class ProjectsListHealthCheckTest {
  private final DisabledMetricMaker metricMaker = new DisabledMetricMaker();
  @Inject private ListeningExecutorService executor;

  @Before
  public void setUp() {
    Guice.createInjector(new HealthCheckModule()).injectMembers(this);
  }

  @Test
  public void shouldBeHealthyWhenListProjectsWorks() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(executor, getWorkingProjectList(0), metricMaker);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldBeUnhealthyWhenListProjectsIsFailing() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(executor, getFailingProjectList(), metricMaker);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldBeUnhealthyWhenListProjectsIsTimingOut() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(
            executor, getWorkingProjectList(AbstractHealthCheck.CHECK_TIMEOUT * 2), metricMaker);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.TIMEOUT);
  }

  private ListProjects getFailingProjectList() {
    return new ListProjects(null, null, null, null, null, null, null, null) {
      @Override
      public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
        throw new IllegalArgumentException("Unable to return project list");
      }
    };
  }

  private ListProjects getWorkingProjectList(long execTime) {
    return new ListProjects(null, null, null, null, null, null, null, null) {
      @Override
      public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
        SortedMap<String, ProjectInfo> projects = new TreeMap<>();
        projects.put("testproject", new ProjectInfo());
        try {
          Thread.sleep(execTime);
        } catch (InterruptedException e) {
          throw new IllegalStateException(e);
        }
        return projects;
      }
    };
  }
}
