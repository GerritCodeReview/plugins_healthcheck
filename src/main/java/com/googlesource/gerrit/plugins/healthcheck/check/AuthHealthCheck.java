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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.AUTH;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.account.Realm;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AuthHealthCheck extends AbstractHealthCheck {
  private static final Logger log = LoggerFactory.getLogger(AuthHealthCheck.class);
  private final Realm realm;
  private final AccountCache byIdCache;
  private final String username;
  private final String password;

  @Inject
  public AuthHealthCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      Realm realm,
      AccountCache byIdCache,
      HealthCheckMetrics.Factory healthCheckMetricsFactory) {
    super(executor, config, AUTH, healthCheckMetricsFactory);

    this.realm = realm;
    this.byIdCache = byIdCache;
    this.username = config.getUsername(AUTH);
    this.password = config.getPassword(AUTH);
  }

  @Override
  protected Result doCheck() throws Exception {
    AuthRequest authRequest = AuthRequest.forUser(username);
    authRequest.setPassword(password);
    realm.authenticate(authRequest);

    Optional<AccountState> accountState = byIdCache.getByUsername(username);
    if (!accountState.isPresent()) {
      log.error("Cannot load account state for username " + username);
      return Result.FAILED;
    }
    if (!accountState.get().account().isActive()) {
      log.error("Authentication error, account " + username + " is inactive");
      return Result.FAILED;
    }
    return Result.PASSED;
  }
}
