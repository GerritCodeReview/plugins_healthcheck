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

import com.google.gson.annotations.SerializedName;

public interface HealthCheck {

  public enum Result {
    @SerializedName("passed")
    PASSED,
    @SerializedName("failed")
    FAILED,
    @SerializedName("timeout")
    TIMEOUT;
  }

  public class Status {
    public final Result result;
    public final long ts;
    public final long elapsed;

    public Status(Result result, long ts, long elapsed) {
      this.result = result;
      this.ts = ts;
      this.elapsed = elapsed;
    }

    protected Boolean isFailure() {
      return this.result != Result.PASSED;
    }
  }

  Status run();

  String name();
}
