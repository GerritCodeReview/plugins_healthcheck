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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BlockedThreadsConfiguratorConfigsTest {
  private final String[] input;
  private final Collection<BlockedThreadsConfigurator.Threshold> expected;

  public BlockedThreadsConfiguratorConfigsTest(
      String[] input, Collection<BlockedThreadsConfigurator.Threshold> expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void shouldReturnExpectedConfig() {
    Collection<BlockedThreadsConfigurator.Threshold> result =
        BlockedThreadsConfigurator.getConfig(input);
    assertThat(result).containsExactlyElementsIn(expected);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    return Arrays.asList(
        new Object[][] {
          {new String[] {}, specs(threshold(DEFAULT_BLOCKED_THREADS_THRESHOLD))},
          {new String[] {"30"}, specs(threshold(30))},
          {
            new String[] {"prefix1=40", "prefix2=70", "prefix3 = 80"},
            specs(threshold("prefix1", 40), threshold("prefix2", 70), threshold("prefix3", 80))
          },
          // the latter configuration is selected
          {new String[] {"30", "40"}, specs(threshold(40))},
          // the latter configuration is selected
          {new String[] {"prefix1=40", "prefix1=70"}, specs(threshold("prefix1", 70))},
          // specific prefix configuration is favored over the global one
          {new String[] {"30", "prefix1=40"}, specs(threshold("prefix1", 40))},
          // specific prefix configuration is favored over the global one and it is deduplicated
          {new String[] {"30", "prefix1=40", "prefix1=70"}, specs(threshold("prefix1", 70))},
        });
  }

  private static BlockedThreadsConfigurator.Threshold threshold(int value) {
    return threshold(null, value);
  }

  private static BlockedThreadsConfigurator.Threshold threshold(String prefix, int value) {
    return new BlockedThreadsConfigurator.Threshold(prefix, value);
  }

  private static Collection<BlockedThreadsConfigurator.Threshold> specs(
      BlockedThreadsConfigurator.Threshold... thresholds) {
    return Arrays.asList(thresholds);
  }
}
