// Copyright (C) 2023 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.FAILED;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.PASSED;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.LUCENEINDEXWRITABLE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.index.SchemaDefinitions;
import com.google.gerrit.index.project.ProjectSchemaDefinitions;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.account.AccountSchemaDefinitions;
import com.google.gerrit.server.index.change.ChangeSchemaDefinitions;
import com.google.gerrit.server.index.group.GroupSchemaDefinitions;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class LuceneIndexWritableCheck extends AbstractHealthCheck {

  private final SitePaths sitePaths;

  @Inject
  public LuceneIndexWritableCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      SitePaths sitePaths) {
    super(executor, config, LUCENEINDEXWRITABLE, healthCheckMetricsFactory);
    this.sitePaths = sitePaths;
  }

  @Override
  protected Result doCheck() throws Exception {
    List<Path> indexDirs =
        Arrays.asList(
            getDir(AccountSchemaDefinitions.INSTANCE),
            getDir(GroupSchemaDefinitions.INSTANCE),
            getDir(ProjectSchemaDefinitions.INSTANCE),
            getDir(ChangeSchemaDefinitions.INSTANCE).resolve("open"),
            getDir(ChangeSchemaDefinitions.INSTANCE).resolve("closed"));
    if (indexDirs.stream().allMatch(this::checkIndex)) {
      return PASSED;
    }
    return FAILED;
  }

  private boolean checkIndex(Path indexDir) {
    File lockFile = indexDir.resolve("write.lock").toFile();
    System.out.println(lockFile);
    return lockFile.exists() && lockFile.canWrite();
  }

  @VisibleForTesting
  public <A> Path getDir(SchemaDefinitions<A> definitions) {
    return sitePaths.index_dir.resolve(
        String.format("%s_%04d", definitions.getName(), definitions.getLatest().getVersion()));
  }
}
