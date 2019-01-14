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

public interface HealthCheck {

  public class Result {
    public final boolean healthy;
    public final long ts;
    public final long elapsed;

    public Result(boolean healthy, long ts, long elapsed) {
      this.healthy = healthy;
      this.ts = ts;
      this.elapsed = elapsed;
    }
  }

  Result run();

  String name();
}
