// Copyright (C) 2024 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.healthcheck.filter;

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.util.http.testutil.FakeHttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class HealthCheckStatusFilterTest {
  private static final String HEALTHCHECK_PATH = "/config/server/healthcheck~status";

  @Test
  public void statusRequestsShouldBeHandled() {
    HealthCheckStatusFilter filterWithoutPrefix = createFilter("http://*:8080/");
    assertThat(filterWithoutPrefix.isStatusCheck(createRequest(HEALTHCHECK_PATH))).isTrue();
    assertThat(filterWithoutPrefix.isStatusCheck(createRequest("/a" + HEALTHCHECK_PATH))).isTrue();
    assertThat(filterWithoutPrefix.isStatusCheck(createRequest("/a/config/server/version")))
        .isFalse();

    String prefix = "/gerrit";
    HealthCheckStatusFilter filterWithPrefix = createFilter("http://*:8080" + prefix + "/");
    assertThat(filterWithPrefix.isStatusCheck(createRequest(HEALTHCHECK_PATH))).isFalse();
    assertThat(filterWithPrefix.isStatusCheck(createRequest("/a" + HEALTHCHECK_PATH))).isFalse();
    assertThat(filterWithPrefix.isStatusCheck(createRequest(prefix + HEALTHCHECK_PATH))).isTrue();
    assertThat(filterWithPrefix.isStatusCheck(createRequest(prefix + "/a" + HEALTHCHECK_PATH)))
        .isTrue();
    assertThat(filterWithPrefix.isStatusCheck(createRequest(prefix + "/a/config/server/version")))
        .isFalse();
  }

  private HealthCheckStatusFilter createFilter(String listenUrl) {
    Config cfg = new Config();
    cfg.setString("httpd", null, "listenUrl", listenUrl);
    return new HealthCheckStatusFilter(null, "healthcheck", cfg);
  }

  private HttpServletRequest createRequest(String path) {
    return new FakeHttpServletRequest("gerrit.example.com", 8080, "", "").setPathInfo(path);
  }
}
