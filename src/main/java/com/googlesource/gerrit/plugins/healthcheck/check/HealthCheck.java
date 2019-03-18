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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface HealthCheck {

  public enum Result {
    @SerializedName("passed")
    PASSED,
    @SerializedName("failed")
    FAILED,
    @SerializedName("timeout")
    TIMEOUT,
    @SerializedName("disabled")
    DISABLED;
  }

  public class StatusSummary {
    public static final StatusSummary INITIAL_STATUS =
        new StatusSummary(Result.PASSED, System.currentTimeMillis(), 0L);
    public final Result result;
    public final long ts;
    public final long elapsed;
    public static final Set<Result> failingResults =
        new HashSet<>(Arrays.asList(Result.FAILED, Result.TIMEOUT));

    public StatusSummary(Result result, long ts, long elapsed) {
      this.result = result;
      this.ts = ts;
      this.elapsed = elapsed;
    }

    public Boolean isFailure() {
      return failingResults.contains(this.result);
    }
  }

  StatusSummary run();

  String name();

  StatusSummary getLatestStatus();
}
