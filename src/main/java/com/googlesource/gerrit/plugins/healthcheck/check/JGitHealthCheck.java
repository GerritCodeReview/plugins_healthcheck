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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.JGIT;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

public class JGitHealthCheck extends AbstractHealthCheck {
  private final GitRepositoryManager repositoryManager;
  private final AllProjectsName allProjectsName;

  @Inject
  public JGitHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      GitRepositoryManager repositoryManager,
      AllProjectsName allProjectsName) {
    super(executor, config, JGIT);
    this.repositoryManager = repositoryManager;
    this.allProjectsName = allProjectsName;
  }

  @Override
  protected Result doCheck() throws Exception {
    try (Repository allProjects = repositoryManager.openRepository(allProjectsName)) {
      ObjectId headObj = allProjects.resolve("refs/meta/config");
      allProjects.open(headObj).getType();
      return Result.PASSED;
    }
  }
}
