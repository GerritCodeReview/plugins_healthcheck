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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.GitSpaceCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.JGitHealthCheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HealthCheckConfigTest {

  private Path tempSitePath;

  private static Path createTempSitePath() throws IOException {
    Path tmp = Files.createTempFile("gerrit_", "_site");
    Files.deleteIfExists(tmp);
    return tmp;
  }

  @Before
  public void setUp() throws IOException {
    tempSitePath = createTempSitePath();
  }

  @After
  public void tearDown() throws IOException {
    if (tempSitePath != null && Files.exists(tempSitePath)) {
      Files.delete(tempSitePath);
    }
  }

  @Test
  public void shouldHaveDefaultTimeout() {
    long defaultTimeout = DEFAULT_CONFIG.getTimeout(null);
    assertThat(defaultTimeout).isGreaterThan(0L);
    assertThat(DEFAULT_CONFIG.getTimeout("fooCheck")).isEqualTo(defaultTimeout);
  }

  @Test
  public void shouldHaveGlobalTimeout() {
    HealthCheckConfig config = new HealthCheckConfig("[healthcheck]\n" + "timeout=1000");

    assertThat(config.getTimeout(null)).isEqualTo(1000);
    assertThat(config.getTimeout("barCheck")).isEqualTo(1000);
  }

  @Test
  public void shouldHaveAuthUsername() {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"auth\"]\n" + "username=test_user");

    assertThat(config.getUsername("auth")).isEqualTo("test_user");
  }

  @Test
  public void shouldHaveAuthPassword() {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"auth\"]\n" + "password=secret");

    assertThat(config.getPassword("auth")).isEqualTo("secret");
  }

  @Test
  public void shouldHaveCheckOverriddenTimeout() {
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck]\n" + "timeout=2000\n" + "[healthcheck \"fooCheck\"]\n" + "timeout=1000");

    assertThat(config.getTimeout("fooCheck")).isEqualTo(1000);
    assertThat(config.getTimeout("barCheck")).isEqualTo(2000);
  }

  @Test
  public void shouldHaveAnEnabledValue() throws IOException {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"fooCheck\"]\n" + "enabled=false");

    assertThat(config.healthCheckEnabled("fooCheck", getCheckEnabledByDefault())).isEqualTo(false);
  }

  @Test
  public void shouldHaveEnabledAndDisabledValue() throws IOException {
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck \"fooCheck\"]\n"
                + "enabled=false\n"
                + "[healthcheck \"barCheck\"]\n"
                + "timeout=1000"
                + "[healthcheck \"bazCheck\"]\n"
                + "enabled=true\n");

    assertThat(config.healthCheckEnabled("fooCheck", getCheckEnabledByDefault())).isEqualTo(false);
    assertThat(config.healthCheckEnabled("barCheck", getCheckEnabledByDefault())).isEqualTo(true);
    assertThat(config.healthCheckEnabled("bazCheck", getCheckEnabledByDefault())).isEqualTo(true);
  }

  @Test
  public void shouldHonourDefaultEnabledValue() throws IOException {
    HealthCheckConfig config = new HealthCheckConfig("[healthcheck \"fooCheck\"]");

    assertThat(config.healthCheckEnabled("gitSpace", getCheckDisabledByDefault())).isEqualTo(false);
    assertThat(config.healthCheckEnabled("jgit", getCheckEnabledByDefault())).isEqualTo(true);
  }

  private AbstractHealthCheck getCheckDisabledByDefault() throws IOException {
    return new GitSpaceCheck(
        new Config(),
        new SitePaths(tempSitePath),
        MoreExecutors.newDirectExecutorService(),
        DEFAULT_CONFIG,
        new DisabledMetricMaker());
  }

  private AbstractHealthCheck getCheckEnabledByDefault() throws IOException {
    return new JGitHealthCheck(
        MoreExecutors.newDirectExecutorService(),
        DEFAULT_CONFIG,
        new InMemoryRepositoryManager(),
        new DisabledMetricMaker());
  }
}
