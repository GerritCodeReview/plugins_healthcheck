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
  public void shouldHaveCheckOverriddenTimeout() {
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck]\n" + "timeout=2000\n" + "[healthcheck \"fooCheck\"]\n" + "timeout=1000");

    assertThat(config.getTimeout("fooCheck")).isEqualTo(1000);
    assertThat(config.getTimeout("barCheck")).isEqualTo(2000);
  }
}
