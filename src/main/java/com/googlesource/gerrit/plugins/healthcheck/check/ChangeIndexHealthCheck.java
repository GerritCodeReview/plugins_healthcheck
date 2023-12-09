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

import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.events.ChangeIndexedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeIndexHealthCheck implements ChangeIndexedListener {
  public static final AbstractModule module() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        DynamicSet.bind(binder(), ChangeIndexedListener.class)
            .to(ChangeIndexHealthCheck.class)
            .in(Singleton.class);
      }
    };
  }

  private static final Logger log = LoggerFactory.getLogger(ChangeIndexHealthCheck.class);

  private final OneOffRequestContext oneOffCtx;
  private final Provider<QueryChanges> queryChangesProvider;
  private final Provider<ChangeIndexer> indexerProvider;

  private AtomicBoolean changeIndexRunning = new AtomicBoolean(false);

  @Inject
  ChangeIndexHealthCheck(
      OneOffRequestContext oneOffCtx,
      Provider<QueryChanges> queryChangesProvider,
      Provider<ChangeIndexer> indexerProvider) {
    this.oneOffCtx = oneOffCtx;
    this.queryChangesProvider = queryChangesProvider;
    this.indexerProvider = indexerProvider;
  }

  @Override
  public void onChangeIndexed(String projectName, int id) {
    changeIndexRunning.set(true);
  }

  @Override
  public void onChangeDeleted(int id) {
    changeIndexRunning.set(true);
  }

  Result check() throws Exception {
    boolean isRunning = changeIndexRunning.getAndSet(false);
    log.info("Index was already running: {}", isRunning);
    return isRunning ? Result.PASSED : callReindex();
  }

  private Result callReindex() throws RestApiException, PermissionBackendException {
    try (ManualRequestContext ctx = oneOffCtx.open()) {
      QueryChanges queryChanges = queryChangesProvider.get();
      queryChanges.setLimit(1);
      queryChanges.addQuery("status:open or status:closed");
      queryChanges.setStart(0);

      @SuppressWarnings("unchecked")
      List<ChangeInfo> changes = (List<ChangeInfo>) queryChanges.apply(null).value();
      if (changes == null || changes.isEmpty()) {
        log.warn("Cannot check the changes index health: received an empty list of changes");
        return Result.FAILED;
      }

      ChangeInfo change = changes.get(0);
      indexerProvider.get().index(Project.nameKey(change.project), Change.id(change._number));
    }
    return Result.PASSED;
  }
}
