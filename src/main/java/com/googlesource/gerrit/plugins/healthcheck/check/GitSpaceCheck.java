// Copyright (C) 2025 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.GIT_SPACE;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;

@Singleton
public class GitSpaceCheck extends AbstractHealthCheck {
  private final Path gitPath;

  @Inject
  public GitSpaceCheck(
      @GerritServerConfig Config gerritConfig,
      SitePaths site,
      ListeningExecutorService executor,
      HealthCheckConfig config,
      MetricMaker metricMaker) {
    super(executor, config, GIT_SPACE, metricMaker);

    this.gitPath = site.resolve(gerritConfig.getString("gerrit", null, "basePath"));
  }

  protected Path getGitPath() {
    return gitPath;
  }

  protected Result doCheck() throws Exception {
    int minDiskFreePercent = config.getMinDiskFreePercent(GIT_SPACE);
    long total = getGitPath().toFile().getTotalSpace();

    if (total <= 0) {
      return Result.FAILED;
    }

    long usable = getGitPath().toFile().getUsableSpace();
    double freePercent = (usable * 100.0) / total;

    return freePercent < minDiskFreePercent ? Result.FAILED : Result.PASSED;
  }
}
