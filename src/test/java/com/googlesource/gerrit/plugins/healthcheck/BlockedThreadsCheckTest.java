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
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKEDTHREADS;
import static java.util.Collections.nCopies;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck.ThreadBeanProvider;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedThreadsCheckTest {
  private static final String HEALTHCHECK_CONFIG_BODY_THRESHOLD =
      "[healthcheck \"" + BLOCKEDTHREADS + "\"]\n" + "  threshold = ";
  private static final HealthCheckConfig CONFIG_THRESHOLD_25 =
      new HealthCheckConfig(HEALTHCHECK_CONFIG_BODY_THRESHOLD + "25");
  private static final HealthCheckConfig CONFIG_THRESHOLD_33 =
      new HealthCheckConfig(HEALTHCHECK_CONFIG_BODY_THRESHOLD + "33");

  @Mock BlockedThreadsCheck.ThreadBeanProvider threadBeanProviderMock;

  @Mock ThreadMXBean beanMock;

  private Injector testInjector;

  @Before
  public void setUp() {
    when(threadBeanProviderMock.get()).thenReturn(beanMock);
    testInjector = createTestInjector(HealthCheckConfig.DEFAULT_CONFIG);
  }

  @Test
  public void shouldPassCheckWhenNoThreadsAreReturned() {
    BlockedThreadsCheck objectUnderTest = createCheck();
    when(beanMock.getThreadInfo(null, 0)).thenReturn(new ThreadInfo[0]);
    assertThat(objectUnderTest.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenNoThreadsAreBlocked() {
    int running = 1;
    int blocked = 0;
    mockThreadsAndCheckResult(running, blocked, Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenBlockedThreadsAreLessThanDefaultThreshold() {
    int running = 2;
    int blocked = 1;
    mockThreadsAndCheckResult(running, blocked, Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenBlockedThreadsAreEqualToDefaultThreshold() {
    int running = 1;
    int blocked = 1;
    mockThreadsAndCheckResult(running, blocked, Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenBlockedThreadsAreAboveTheDefaultThreshold() {
    int running = 1;
    int blocked = 2;
    mockThreadsAndCheckResult(running, blocked, Result.FAILED);
  }

  @Test
  public void shouldPassCheckWhenBlockedThreadsAreLessThenThreshold() {
    int running = 3;
    int blocked = 1;
    testInjector = createTestInjector(CONFIG_THRESHOLD_25);

    mockThreadsAndCheckResult(running, blocked, Result.PASSED);
  }

  @Test
  public void shouldFailCheckWhenBlockedThreadsAreAboveTheThreshold() {
    int running = 1;
    int blocked = 1;
    testInjector = createTestInjector(CONFIG_THRESHOLD_33);

    mockThreadsAndCheckResult(running, blocked, Result.FAILED);
  }

  @Test
  public void shouldPassCheckWhenBlockedThreadsWithPrefixAreLessThenThreshold() {
    int running = 3;
    int blocked = 1;
    String prefix = "blocked-threads-prefix";
    testInjector = createTestInjector(CONFIG_THRESHOLD_25);

    mockThreadsAndCheckResult(running, blocked, Result.PASSED, prefix);
  }

  @Test
  public void shouldFailCheckWhenBlockedThreadsWithPrefixAreAboveTheThreshold() {
    int running = 1;
    int blocked = 1;
    String prefix = "blocked-threads-prefix";
    testInjector = createTestInjector(CONFIG_THRESHOLD_33);

    mockThreadsAndCheckResult(running, blocked, Result.FAILED, prefix);
  }

  @Test
  public void shouldFailCheckWhenAnyOfTheBlockedThreadsWithPrefixAreAboveTheThreshold() {
    int running = 1;
    int blocked = 1;
    String blockedPrefix = "blocked-threads-prefix";
    String notBlockedPrefix = "running-threads";
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck \""
                + BLOCKEDTHREADS
                + "\"]\n"
                + "  threshold = "
                + blockedPrefix
                + " = 33"
                + "\nthreshold = "
                + notBlockedPrefix
                + "=33");
    testInjector = createTestInjector(config);

    List<ThreadInfo> infos = new ArrayList<>(running + blocked + running);
    infos.addAll(nCopies(running, mockInfo(Thread.State.RUNNABLE, blockedPrefix)));
    infos.addAll(nCopies(blocked, mockInfo(Thread.State.BLOCKED, blockedPrefix)));
    infos.addAll(nCopies(running, mockInfo(Thread.State.RUNNABLE, notBlockedPrefix)));
    when(beanMock.getThreadInfo(null, 0)).thenReturn(infos.toArray(new ThreadInfo[infos.size()]));
    checkResult(Result.FAILED);
  }

  private void mockThreadsAndCheckResult(int running, int blocked, Result expected) {
    mockThreadsAndCheckResult(running, blocked, expected, "some-prefix");
  }

  private void mockThreadsAndCheckResult(int running, int blocked, Result expected, String prefix) {
    mockThreads(running, blocked, prefix);
    checkResult(expected);
  }

  private void checkResult(Result expected) {
    BlockedThreadsCheck objectUnderTest = createCheck();
    assertThat(objectUnderTest.run().result).isEqualTo(expected);
  }

  private void mockThreads(int running, int blocked, String prefix) {
    List<ThreadInfo> infos = new ArrayList<>(running + blocked);
    infos.addAll(nCopies(running, mockInfo(Thread.State.RUNNABLE, prefix)));
    infos.addAll(nCopies(blocked, mockInfo(Thread.State.BLOCKED, prefix)));
    when(beanMock.getThreadInfo(null, 0)).thenReturn(infos.toArray(new ThreadInfo[infos.size()]));
  }

  private ThreadInfo mockInfo(Thread.State state, String prefix) {
    ThreadInfo infoMock = mock(ThreadInfo.class);
    when(infoMock.getThreadState()).thenReturn(state);
    when(infoMock.getThreadName()).thenReturn(prefix);
    return infoMock;
  }

  private BlockedThreadsCheck createCheck() {
    return testInjector.getInstance(BlockedThreadsCheck.class);
  }

  private Injector createTestInjector(HealthCheckConfig config) {
    Injector injector =
        Guice.createInjector(
            new HealthCheckModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(HealthCheckConfig.class).toInstance(config);
                bind(HealthCheckMetrics.Factory.class).to(DummyHealthCheckMetricsFactory.class);
                bind(ThreadBeanProvider.class).toInstance(threadBeanProviderMock);
              }
            },
            BlockedThreadsCheck.SUB_CHECKS);
    return injector;
  }
}
