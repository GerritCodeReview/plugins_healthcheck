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
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.util.Providers;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChangeIndexHealthCheckTest {
  @Mock private OneOffRequestContext oneOffCtxMock;
  @Mock private QueryChanges queryChangesMock;
  @Mock private ChangeIndexer indexerMock;

  private ChangeIndexHealthCheck objectUnderTest;

  @Before
  public void setUp() {
    objectUnderTest =
        new ChangeIndexHealthCheck(
            oneOffCtxMock, Providers.of(queryChangesMock), Providers.of(indexerMock));
  }

  @Test
  public void shouldPassCheckWhenChangeIsIndexed() throws Exception {
    objectUnderTest.onChangeIndexed("some-project", 1);

    assertThat(objectUnderTest.check()).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckWhenChangeIsDeleted() throws Exception {
    objectUnderTest.onChangeDeleted(1);

    assertThat(objectUnderTest.check()).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldPassCheckByCallingReindex() throws Exception {
    mockQueryChangesWithChangeInfo();

    assertThat(objectUnderTest.check()).isEqualTo(Result.PASSED);
    verify(indexerMock).index(any(), any());
  }

  @Test
  public void shouldFailCheckWhenNoChangesToReindexAreAvailable() throws Exception {
    mockQueryChangesWithResponse(Collections.emptyList());

    assertThat(objectUnderTest.check()).isEqualTo(Result.FAILED);
  }

  @Test
  public void shouldThrowWhenCallToReindexThrows() throws Exception {
    mockQueryChangesWithChangeInfo();
    doThrow(new StorageException("test that storage has failed"))
        .when(indexerMock)
        .index(any(), any());

    assertThrows(StorageException.class, () -> objectUnderTest.check());
  }

  private void mockQueryChangesWithChangeInfo()
      throws RestApiException, PermissionBackendException {
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.project = "foo";
    changeInfo._number = 1;

    mockQueryChangesWithResponse(List.of(changeInfo));
  }

  private void mockQueryChangesWithResponse(List<ChangeInfo> response)
      throws RestApiException, PermissionBackendException {
    when(queryChangesMock.apply(any())).thenReturn(Response.ok(response));
  }
}
