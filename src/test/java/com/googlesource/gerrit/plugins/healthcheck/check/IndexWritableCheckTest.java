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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.FAILED;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.PASSED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.DummyHealthCheckMetricsFactory;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IndexWritableCheckTest {

  private static Project.NameKey PROJECT_NAME = Project.NameKey.parse("projectName");
  private static HealthCheckMetrics.Factory healthCheckMetricsFactory =
      new DummyHealthCheckMetricsFactory();
  @Mock private HealthCheckConfig healthCheckConfig;
  @Mock private ChangeIndexer changeIndexer;

  @Before
  public void setupMocks() {
    when(healthCheckConfig.getIndexWritableProjectName()).thenReturn(PROJECT_NAME);
  }

  @Test
  public void shouldPassCheckWhenIndexDoesNotThrow() throws Exception {
    Injector injector = testInjector();

    IndexWritableCheck check =
        new IndexWritableCheck(
            injector.getInstance(ListeningExecutorService.class),
            healthCheckConfig,
            healthCheckMetricsFactory,
            changeIndexer);
    assertThat(check.doCheck()).isEqualTo(PASSED);
  }

  @Test
  public void shouldFailCheckWhenIndexThrows() throws Exception {
    Injector injector = testInjector();
    doThrow(StorageException.class).when(changeIndexer).index(eq(PROJECT_NAME), any());

    IndexWritableCheck check =
        new IndexWritableCheck(
            injector.getInstance(ListeningExecutorService.class),
            healthCheckConfig,
            healthCheckMetricsFactory,
            changeIndexer);
    assertThat(check.doCheck()).isEqualTo(FAILED);
  }

  private Injector testInjector() {
    return Guice.createInjector(new HealthCheckModule());
  }
}
