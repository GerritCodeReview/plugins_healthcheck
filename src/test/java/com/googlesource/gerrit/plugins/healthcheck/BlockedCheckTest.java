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

package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedCheckTest {
  @Mock BlockedCheck.ThreadBeanProvider threadBeanProviderMock;

  @Mock ThreadMXBean beanMock;

  @Before
  public void setUp() {
    when(threadBeanProviderMock.get()).thenReturn(beanMock);

    // by default enable blocked threads detection
    when(beanMock.isSynchronizerUsageSupported()).thenReturn(true);
  }

  @Test
  public void shouldBeDisabledWhenNotSupportedByJava() {
    BlockedCheck objectUnderTest = createCheck(HealthCheckConfig.DEFAULT_CONFIG);
    when(beanMock.isSynchronizerUsageSupported()).thenReturn(false);
    when(beanMock.isObjectMonitorUsageSupported()).thenReturn(false);
    assertThat(objectUnderTest.run().result).isEqualTo(Result.DISABLED);
  }

  @Test
  public void shouldPassCheckWhenNoThreadsAreReturned() {
    BlockedCheck objectUnderTest = createCheck(HealthCheckConfig.DEFAULT_CONFIG);
    assertThat(objectUnderTest.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenNoThreadsAreBlocked() {
    mockThreadsAndCheckResult(1, 0, Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenBlockedLessThanDefaultThreshold() {
    mockThreadsAndCheckResult(2, 1, Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenBlockedEqualToDefaultThreshold() {
    mockThreadsAndCheckResult(1, 1, Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenBlockedAboveTheDefaultThreshold() {
    mockThreadsAndCheckResult(1, 2, Result.FAILED);
  }

  @Test
  public void shouldPassCheckWhenBlockedLessThenThreshold() {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"" + BLOCKED + "\"]\n" + "  threshold = 33");
    mockThreadsAndCheckResult(2, 1, Result.PASSED, config);
  }

  @Test
  public void shouldFailCheckWhenBlockedAboveTheThreshold() {
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"" + BLOCKED + "\"]\n" + "  threshold = 33");
    mockThreadsAndCheckResult(1, 1, Result.FAILED, config);
  }

  private void mockThreadsAndCheckResult(int running, int blocked, Result expected) {
    mockThreadsAndCheckResult(running, blocked, expected, HealthCheckConfig.DEFAULT_CONFIG);
  }

  private void mockThreadsAndCheckResult(
      int running, int blocked, Result expected, HealthCheckConfig config) {
    mockThreads(running, blocked);
    BlockedCheck objectUnderTest = createCheck(config);
    assertThat(objectUnderTest.run().result).isEqualTo(expected);
  }

  private void mockThreads(int running, int blocked) {
    List<ThreadInfo> infos = new ArrayList<>(running + blocked);
    infos.addAll(Collections.nCopies(running, mockInfo(Thread.State.RUNNABLE)));
    infos.addAll(Collections.nCopies(blocked, mockInfo(Thread.State.BLOCKED)));
    when(beanMock.dumpAllThreads(false, true))
        .thenReturn(infos.toArray(new ThreadInfo[infos.size()]));
  }

  private ThreadInfo mockInfo(Thread.State state) {
    ThreadInfo infoMock = mock(ThreadInfo.class);
    when(infoMock.getThreadState()).thenReturn(state);
    return infoMock;
  }

  private BlockedCheck createCheck(HealthCheckConfig config) {
    Injector injector = Guice.createInjector(new HealthCheckModule());
    return new BlockedCheck(
        injector.getInstance(ListeningExecutorService.class),
        config,
        new DummyHealthCheckMetricsFactory(),
        threadBeanProviderMock);
  }
}
