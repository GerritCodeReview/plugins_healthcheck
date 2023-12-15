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

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.CHANGES_INDEX;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.index.IndexType;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ChangesIndexHealthCheck extends AbstractHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(QueryChangesHealthCheck.class);
  private static final String lockFilename = "write.lock";

  private final boolean isLuceneIndex;
  private final Optional<ChangesIndexLockFiles> changes;

  @Inject
  ChangesIndexHealthCheck(
      @GerritServerConfig Config cfg,
      ListeningExecutorService executor,
      HealthCheckConfig config,
      MetricMaker metricMaker,
      SitePaths sitePaths) {
    super(executor, config, CHANGES_INDEX, metricMaker);
    this.isLuceneIndex = isIndexTypeLucene(cfg);
    this.changes = getChangesIndexLockFiles(sitePaths.index_dir);
  }

  @Override
  protected Result doCheck() throws Exception {
    return isLuceneIndex
        ? changes.map(locks -> locks.valid() ? Result.PASSED : Result.FAILED).orElse(Result.FAILED)
        : Result.DISABLED;
  }

  private static boolean isIndexTypeLucene(Config cfg) {
    IndexType indexType = new IndexType(cfg.getString("index", null, "type"));
    boolean isLucene = indexType.isLucene();
    if (!isLucene) {
      log.warn(
          "Configured index type [{}] is not supported for index health check therefore it is disabled.",
          indexType);
    }
    return isLucene;
  }

  private static Optional<ChangesIndexLockFiles> getChangesIndexLockFiles(Path indexDir) {
    FileBasedConfig cfg =
        new FileBasedConfig(indexDir.resolve("gerrit_index.config").toFile(), FS.detect());
    try {
      cfg.load();
      return getActiveIndexVersion(cfg, "changes")
          .map(version -> getChangesLockFiles(indexDir, version));
    } catch (IOException | ConfigInvalidException e) {
      log.error("Getting changes index version from configuration failed", e);
    }
    return Optional.empty();
  }

  private static Optional<String> getActiveIndexVersion(Config cfg, String indexindexPrefix) {
    String section = "index";
    return cfg.getSubsections(section).stream()
        .filter(
            subsection ->
                subsection.startsWith(indexindexPrefix)
                    && cfg.getBoolean(section, subsection, "ready", false))
        .findAny();
  }

  private static ChangesIndexLockFiles getChangesLockFiles(Path indexDir, String indexVersion) {
    Path versionDir = indexDir.resolve(indexVersion);
    return new ChangesIndexLockFiles(
        versionDir.resolve("open").resolve(lockFilename).toFile(),
        versionDir.resolve("closed").resolve(lockFilename).toFile());
  }

  private static class ChangesIndexLockFiles {
    private final File openLock;
    private final File closedLock;

    private ChangesIndexLockFiles(File openLock, File closedLock) {
      this.openLock = openLock;
      this.closedLock = closedLock;
    }

    private boolean valid() {
      return validLock(openLock) && validLock(closedLock);
    }

    private static boolean validLock(File writeLock) {
      return writeLock.isFile() && writeLock.canWrite();
    }
  }
}
