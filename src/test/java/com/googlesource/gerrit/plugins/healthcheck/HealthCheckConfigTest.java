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
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import org.junit.Test;

public class HealthCheckConfigTest {

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
  public void shouldHaveAnEnabledValue() {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"fooCheck\"]\n" + "enabled=false");

    assertThat(config.healthCheckEnabled("fooCheck", new TestHealthCheck(config, true)))
        .isEqualTo(false);
  }

  @Test
  public void shouldHaveEnabledAndDisabledValue() {
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck \"fooCheck\"]\n"
                + "enabled=false\n"
                + "[healthcheck \"barCheck\"]\n"
                + "timeout=1000"
                + "[healthcheck \"bazCheck\"]\n"
                + "enabled=true\n");

    assertThat(config.healthCheckEnabled("fooCheck", new TestHealthCheck(config, true)))
        .isEqualTo(false);
    assertThat(config.healthCheckEnabled("barCheck", new TestHealthCheck(config, true)))
        .isEqualTo(true);
    assertThat(config.healthCheckEnabled("bazCheck", new TestHealthCheck(config, true)))
        .isEqualTo(true);
  }

  @Test
  public void shouldHonourDefaultEnabledValue() {
    HealthCheckConfig config = new HealthCheckConfig("[healthcheck \"fooCheck\"]");

    assertThat(config.healthCheckEnabled("fooCheck", new TestHealthCheck(config, true)))
        .isEqualTo(true);
    assertThat(config.healthCheckEnabled("fooCheck", new TestHealthCheck(config, false)))
        .isEqualTo(false);
  }

  private static class TestHealthCheck extends AbstractHealthCheck {

    private final boolean isEnabledByDefault;

    protected TestHealthCheck(HealthCheckConfig healthCheckConfig, boolean isEnabledByDefault) {
      super(
          MoreExecutors.newDirectExecutorService(),
          healthCheckConfig,
          "test-check",
          new DisabledMetricMaker());
      this.isEnabledByDefault = isEnabledByDefault;
    }

    @Override
    protected Result doCheck() throws Exception {
      return null;
    }

    @Override
    public boolean isEnabledByDefault() {
      return isEnabledByDefault;
    }
  }
}
