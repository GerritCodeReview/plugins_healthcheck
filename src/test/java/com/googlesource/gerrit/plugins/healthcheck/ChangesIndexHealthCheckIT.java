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

package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.CHANGES_INDEX;

import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.Sandboxed;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.junit.Test;

@TestPlugin(
    name = "healthcheck-test",
    sysModule =
        "com.googlesource.gerrit.plugins.healthcheck.AbstractHealthCheckIntegrationTest$TestModule",
    httpModule = "com.googlesource.gerrit.plugins.healthcheck.HttpModule")
@Sandboxed
public class ChangesIndexHealthCheckIT extends AbstractHealthCheckIntegrationTest {
  @Inject SitePaths sitePaths;

  @Test
  @UseLocalDisk
  @GerritConfig(name = "index.type", value = "lucene")
  public void shouldReturnChangesIndexCheck() throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), CHANGES_INDEX, "passed");
  }

  @Test
  @UseLocalDisk
  @GerritConfig(name = "index.type", value = "lucene")
  public void shouldFailWhenOpenChangesIndexWriteLockIsMissing() throws Exception {
    shouldFailWhenChangesIndexWriteLockIsMissing("open");
  }

  @Test
  @UseLocalDisk
  @GerritConfig(name = "index.type", value = "lucene")
  public void shouldFailWhenClosedChangesIndexWriteLockIsMissing() throws Exception {
    shouldFailWhenChangesIndexWriteLockIsMissing("closed");
  }

  private void shouldFailWhenChangesIndexWriteLockIsMissing(String indexType) throws Exception {
    RestResponse resp = getHealthCheckStatus();
    resp.assertOK();

    assertCheckResult(getResponseJson(resp), CHANGES_INDEX, "passed");

    Path openChangesIndexLockPath = getIndexLockFile(sitePaths.index_dir, indexType);
    assertThat(
            openChangesIndexLockPath
                .toFile()
                .renameTo(
                    openChangesIndexLockPath
                        .getParent()
                        .resolve(openChangesIndexLockPath.getFileName().toString() + ".backup")
                        .toFile()))
        .isTrue();

    resp = getHealthCheckStatus();
    resp.assertStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    assertCheckResult(getResponseJson(resp), CHANGES_INDEX, "failed");
  }

  @Test
  @GerritConfig(name = "index.type", value = "lucene")
  public void shouldReturnChangesIndexCheckAsDisabled() throws Exception {
    disableCheck(CHANGES_INDEX);
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), CHANGES_INDEX, "disabled");
  }

  @Test
  @GerritConfig(name = "index.type", value = "fake")
  public void shouldReturnChangesIndexCheckAsDisabledWhenIndexIsNotLucene() throws Exception {
    RestResponse resp = getHealthCheckStatus();

    resp.assertOK();
    assertCheckResult(getResponseJson(resp), CHANGES_INDEX, "disabled");
  }

  private Path getIndexLockFile(Path indexDir, String indexType)
      throws IOException, ConfigInvalidException {
    Path cfgFile = indexDir.resolve("gerrit_index.config");
    FileBasedConfig cfg = new FileBasedConfig(cfgFile.toFile(), FS.detect());
    cfg.load();
    String section = "index";
    String activeVersion =
        cfg.getSubsections(section).stream()
            .filter(
                version ->
                    version.startsWith("changes")
                        && cfg.getBoolean(section, version, "ready", false))
            .findAny()
            .orElseThrow(
                () -> {
                  return new NoSuchElementException(
                      String.format(
                          "There is no changes index version marked as ready in %s file", cfgFile));
                });

    return indexDir.resolve(activeVersion).resolve(indexType).resolve("write.lock");
  }
}
