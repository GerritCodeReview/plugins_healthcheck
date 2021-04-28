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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsConfigurator;
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
  @Mock BlockedThreadsCheck.ThreadBeanProvider threadBeanProviderMock;

  @Mock ThreadMXBean beanMock;

  @Before
  public void setUp() {
    when(threadBeanProviderMock.get()).thenReturn(beanMock);
  }

  @Test
  public void shouldPassCheckWhenNoThreadsAreReturned() {
    BlockedThreadsCheck objectUnderTest = createCheck(HealthCheckConfig.DEFAULT_CONFIG);
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
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"" + BLOCKEDTHREADS + "\"]\n" + "  threshold = 25");
    mockThreadsAndCheckResult(running, blocked, Result.PASSED, config);
  }

  @Test
  public void shouldFailCheckWhenBlockedThreadsAreAboveTheThreshold() {
    int running = 1;
    int blocked = 1;
    HealthCheckConfig config =
        new HealthCheckConfig("[healthcheck \"" + BLOCKEDTHREADS + "\"]\n" + "  threshold = 33");
    mockThreadsAndCheckResult(running, blocked, Result.FAILED, config);
  }

  @Test
  public void shouldPassCheckWhenBlockedThreadsWithPrefixAreLessThenThreshold() {
    int running = 3;
    int blocked = 1;
    String prefix = "blocked-threads-prefix";
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck \"" + BLOCKEDTHREADS + "\"]\n" + "  threshold = " + prefix + " = 25");
    mockThreadsAndCheckResult(running, blocked, Result.PASSED, prefix, config);
  }

  @Test
  public void shouldFailCheckWhenBlockedThreadsWithPrefixAreAboveTheThreshold() {
    int running = 1;
    int blocked = 1;
    String prefix = "blocked-threads-prefix";
    HealthCheckConfig config =
        new HealthCheckConfig(
            "[healthcheck \"" + BLOCKEDTHREADS + "\"]\n" + "  threshold = " + prefix + " = 33");
    mockThreadsAndCheckResult(running, blocked, Result.FAILED, prefix, config);
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
    List<ThreadInfo> infos = new ArrayList<>(running + blocked + running);
    infos.addAll(nCopies(running, mockInfo(Thread.State.RUNNABLE, blockedPrefix)));
    infos.addAll(nCopies(blocked, mockInfo(Thread.State.BLOCKED, blockedPrefix)));
    infos.addAll(nCopies(running, mockInfo(Thread.State.RUNNABLE, notBlockedPrefix)));
    when(beanMock.getThreadInfo(null, 0)).thenReturn(infos.toArray(new ThreadInfo[infos.size()]));
    checkResult(Result.FAILED, config);
  }

  private void mockThreadsAndCheckResult(int running, int blocked, Result expected) {
    mockThreadsAndCheckResult(running, blocked, expected, HealthCheckConfig.DEFAULT_CONFIG);
  }

  private void mockThreadsAndCheckResult(
      int running, int blocked, Result expected, HealthCheckConfig config) {
    mockThreadsAndCheckResult(running, blocked, expected, "some-prefix", config);
  }

  private void mockThreadsAndCheckResult(
      int running, int blocked, Result expected, String prefix, HealthCheckConfig config) {
    mockThreads(running, blocked, prefix);
    checkResult(expected, config);
  }

  private void checkResult(Result expected, HealthCheckConfig config) {
    BlockedThreadsCheck objectUnderTest = createCheck(config);
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

  private BlockedThreadsCheck createCheck(HealthCheckConfig config) {
    DummyHealthCheckMetricsFactory checkMetricsFactory = new DummyHealthCheckMetricsFactory();
    Injector injector =
        Guice.createInjector(
            new HealthCheckModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(HealthCheckConfig.class).toInstance(config);
                bind(HealthCheckMetrics.Factory.class).toInstance(checkMetricsFactory);
              }
            },
            BlockedThreadsCheck.SUB_CHECKS);
    return new BlockedThreadsCheck(
        injector.getInstance(ListeningExecutorService.class),
        config,
        checkMetricsFactory,
        threadBeanProviderMock,
        Providers.of(injector.getInstance(BlockedThreadsConfigurator.class)));
  }
}
