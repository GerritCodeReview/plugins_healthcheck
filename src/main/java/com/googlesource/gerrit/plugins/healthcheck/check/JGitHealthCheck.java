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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

@Singleton
public class JGitHealthCheck extends AbstractHealthCheck {
  private final GitRepositoryManager repositoryManager;
  private final ImmutableList<Project.NameKey> systemRepoNameKeys;

  @Inject
  public JGitHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      GitRepositoryManager repositoryManager,
      AllProjectsName allProjectsName,
      AllUsersName allUsersName) {
    super(executor, config, JGIT);

    this.repositoryManager = repositoryManager;
    this.systemRepoNameKeys = ImmutableList.of(allProjectsName, allUsersName);
  }

  @Override
  protected Result doCheck() throws Exception {
    for(Project.NameKey repoNameKey : this.systemRepoNameKeys) {
        Repository repo = repositoryManager.openRepository(repoNameKey);
        ObjectId headObj = repo.resolve("refs/meta/config");
        repo.open(headObj).getType();
    }
    return Result.PASSED;
  }
}
