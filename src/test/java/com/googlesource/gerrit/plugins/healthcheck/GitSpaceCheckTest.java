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

package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.config.SitePaths;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.GitSpaceCheck;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GitSpaceCheckTest {
  private File mockFile;
  private Path tempSitePath;

  private static Path createTempSitePath() throws IOException {
    Path tmp = Files.createTempFile("gerrit_", "_site");
    Files.deleteIfExists(tmp);
    return tmp;
  }

  @Before
  public void setUp() throws IOException {
    mockFile = mock(File.class);
    tempSitePath = createTempSitePath();
  }

  @After
  public void tearDown() throws IOException {
    if (tempSitePath != null && Files.exists(tempSitePath)) {
      Files.delete(tempSitePath);
    }
  }

  private GitSpaceCheck createGitSpaceCheck(String configContent) throws IOException {
    HealthCheckConfig config = new HealthCheckConfig(configContent);
    SitePaths sitePaths = new SitePaths(tempSitePath);
    return new GitSpaceCheck(
        new Config(),
        sitePaths,
        MoreExecutors.newDirectExecutorService(),
        config,
        new DisabledMetricMaker()) {
      @Override
      protected File getGitDirectory() {
        return mockFile;
      }
    };
  }

  private GitSpaceCheck createGitSpaceCheck() throws IOException {
    Config config = new Config();
    config.setBoolean("healthcheck", "gitspace", "enabled", true);
    return createGitSpaceCheck(config.toText());
  }

  @Test
  public void shouldPassWhenUsableDiskAboveThreshold() throws Exception {
    GitSpaceCheck gitSpaceCheck = createGitSpaceCheck();
    when(mockFile.getTotalSpace()).thenReturn(100L);
    when(mockFile.getUsableSpace()).thenReturn(20L);

    assertThat(gitSpaceCheck.run().result()).isEqualTo(AbstractHealthCheck.Result.PASSED);
  }

  @Test
  public void shouldFailWhenUsableDiskBelowThreshold() throws Exception {
    GitSpaceCheck gitSpaceCheck = createGitSpaceCheck();
    when(mockFile.getTotalSpace()).thenReturn(100L);
    when(mockFile.getUsableSpace()).thenReturn(5L);

    assertThat(gitSpaceCheck.run().result()).isEqualTo(AbstractHealthCheck.Result.FAILED);
  }

  @Test
  public void shouldComplyWithNonDefaultThreshold() throws Exception {
    Config config = new Config();
    config.setBoolean("healthcheck", "gitspace", "enabled", true);
    config.setInt("healthcheck", "gitspace", "minFreeDiskPercent", 50);
    config.toText();

    GitSpaceCheck gitSpaceCheck = createGitSpaceCheck(config.toText());

    when(mockFile.getTotalSpace()).thenReturn(100L);
    when(mockFile.getUsableSpace()).thenReturn(5L);
    assertThat(gitSpaceCheck.run().result()).isEqualTo(AbstractHealthCheck.Result.FAILED);

    when(mockFile.getUsableSpace()).thenReturn(60L);
    assertThat(gitSpaceCheck.run().result()).isEqualTo(AbstractHealthCheck.Result.PASSED);
  }

  @Test
  public void shouldFailWhenTotalSpaceIsZero() throws Exception {
    GitSpaceCheck gitSpaceCheck = createGitSpaceCheck();
    when(mockFile.getTotalSpace()).thenReturn(0L);

    assertThat(gitSpaceCheck.run().result()).isEqualTo(AbstractHealthCheck.Result.FAILED);
  }
}
