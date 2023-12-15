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

import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
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
class EnabledChangesIndexHealthCheck implements ChangesIndexHealthCheck.Checker {
  private static final Logger log = LoggerFactory.getLogger(EnabledChangesIndexHealthCheck.class);

  private final Optional<ChangesIndexLockFiles> changes;

  @Inject
  EnabledChangesIndexHealthCheck(SitePaths sitePaths) {
    changes = getChangesIndexesLockFiles(sitePaths.index_dir);
  }

  @Override
  public Result doCheck() throws Exception {
    return changes
        .map(locks -> locks.valid() ? Result.PASSED : Result.FAILED)
        .orElse(Result.FAILED);
  }

  private static Optional<ChangesIndexLockFiles> getChangesIndexesLockFiles(Path indexDir) {
    FileBasedConfig cfg =
        new FileBasedConfig(indexDir.resolve("gerrit_index.config").toFile(), FS.detect());
    try {
      cfg.load();
      return getActiveIndexVersion(cfg, "changes")
          .map(version -> getChangesLockFiles(indexDir, version));
    } catch (IOException | ConfigInvalidException e) {
      log.error("Getting changes index version from confguration failed", e);
    }
    return Optional.empty();
  }

  private static Optional<String> getActiveIndexVersion(Config cfg, String index) {
    String section = "index";
    return cfg.getSubsections(section).stream()
        .filter(
            subsection ->
                subsection.startsWith(index) && cfg.getBoolean(section, subsection, "ready", false))
        .findAny();
  }

  private static ChangesIndexLockFiles getChangesLockFiles(Path indexDir, String indexVersion) {
    Path versionDir = indexDir.resolve(indexVersion);
    String lockFilename = "write.lock";
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
