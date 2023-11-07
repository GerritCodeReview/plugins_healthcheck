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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.INDEXWRITABLE;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import org.eclipse.jgit.errors.ConfigInvalidException;

public class IndexWritableCheck extends AbstractHealthCheck {

  private final ChangeIndexer changeIndexer;
  private final Project.NameKey projectName;
  private final Change.Id changeId;

  @Inject
  public IndexWritableCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      MetricMaker metricMaker,
      ChangeIndexer changeIndexer)
      throws ConfigInvalidException {
    super(executor, config, INDEXWRITABLE, metricMaker);
    this.changeIndexer = changeIndexer;
    this.projectName = config.getIndexWritableProjectName();
    this.changeId = config.getIndexWritableChangeId();
  }

  @Override
  protected Result doCheck() throws Exception {
    try {
      changeIndexer.index(projectName, changeId);
    } catch (StorageException e) {
      return Result.FAILED;
    }
    return Result.PASSED;
  }
}
