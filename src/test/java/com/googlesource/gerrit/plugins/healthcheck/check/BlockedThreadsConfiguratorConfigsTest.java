// Copyright (C) 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsConfigurator.DEFAULT_BLOCKED_THREADS_THRESHOLD;
import static java.util.Collections.emptyList;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BlockedThreadsConfiguratorConfigsTest {
  private final String[] input;
  private final BlockedThreadsConfigurator.Config expected;

  public BlockedThreadsConfiguratorConfigsTest(
      String[] input, BlockedThreadsConfigurator.Config expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void shouldReturnExpectedConfig() {
    BlockedThreadsConfigurator.Config result = BlockedThreadsConfigurator.getConfig(input);
    assertThat(result).isNotNull();
    assertThat(result.threshold).isEqualTo(expected.threshold);
    assertThat(result.specs).containsExactlyElementsIn(expected.specs);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(
        new Object[][] {
          {
            new String[] {},
            new BlockedThreadsConfigurator.Config(DEFAULT_BLOCKED_THREADS_THRESHOLD, emptyList())
          },
          {new String[] {"30"}, new BlockedThreadsConfigurator.Config(30, emptyList())},
          {new String[] {"40%"}, new BlockedThreadsConfigurator.Config(40, emptyList())},
          {
            new String[] {"prefix1", "prefix2=40", "prefix3=70%", "prefix4 = 80"},
            new BlockedThreadsConfigurator.Config(
                DEFAULT_BLOCKED_THREADS_THRESHOLD,
                ImmutableList.of(
                    new BlockedThreadsConfigurator.Threshold("prefix1", null),
                    new BlockedThreadsConfigurator.Threshold("prefix2", 40),
                    new BlockedThreadsConfigurator.Threshold("prefix3", 70),
                    new BlockedThreadsConfigurator.Threshold("prefix4", 80)))
          }
        });
  }
}
