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
import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig.DEFAULT_CONFIG;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.gerrit.server.restapi.project.ListProjectsImpl;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.ProjectsListHealthCheck;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectsListHealthCheckTest {
  @Inject private ListeningExecutorService executor;

  HealthCheckMetrics.Factory healthCheckMetricsFactory = new DummyHealthCheckMetricsFactory();

  private Config gerritConfig = new Config();

  @Mock private OneOffRequestContext mockOneOffCtx;

  @Before
  public void setUp() throws Exception {
    Guice.createInjector(new HealthCheckModule()).injectMembers(this);
  }

  @Test
  public void shouldBeHealthyWhenListProjectsWorks() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(
            executor,
            DEFAULT_CONFIG,
            getWorkingProjectList(0),
            mockOneOffCtx,
            healthCheckMetricsFactory);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldBeUnhealthyWhenListProjectsIsFailing() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(
            executor,
            DEFAULT_CONFIG,
            getFailingProjectList(),
            mockOneOffCtx,
            healthCheckMetricsFactory);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldBeUnhealthyWhenListProjectsIsTimingOut() {
    ProjectsListHealthCheck jGitHealthCheck =
        new ProjectsListHealthCheck(
            executor,
            DEFAULT_CONFIG,
            getWorkingProjectList(DEFAULT_CONFIG.getTimeout() * 2),
            mockOneOffCtx,
            healthCheckMetricsFactory);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.TIMEOUT);
  }

  private Provider<ListProjects> getFailingProjectList() {
    return Providers.of(
        new ListProjectsImpl(
            null, null, null, null, null, null, null, null, null, gerritConfig, null) {

          @Override
          public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
            throw new IllegalArgumentException("Unable to return project list");
          }
        });
  }

  private Provider<ListProjects> getWorkingProjectList(long execTime) {
    return Providers.of(
        new ListProjectsImpl(
            null, null, null, null, null, null, null, null, null, gerritConfig, null) {

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
        });
  }
}
