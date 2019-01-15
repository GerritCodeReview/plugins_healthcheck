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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHealthCheck implements HealthCheck {
  private static final Logger log = LoggerFactory.getLogger(AbstractHealthCheck.class);
  private final String name;

  protected AbstractHealthCheck(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Status run() {
    Result healthy;
    long ts = System.currentTimeMillis();
    try {
      healthy = doCheck();
    } catch (Exception e) {
      log.warn("Check {} failed", name, e);
      healthy = Result.FAILED;
    }
    return new Status(healthy, ts, System.currentTimeMillis() - ts);
  }

  protected abstract Result doCheck() throws Exception;
}
