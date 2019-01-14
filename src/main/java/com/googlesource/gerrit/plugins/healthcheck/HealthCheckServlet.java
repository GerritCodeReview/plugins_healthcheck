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

package com.googlesource.gerrit.plugins.healthcheck;

import static com.googlesource.gerrit.plugins.healthcheck.HealthCheck.REVIEWDB;

import com.google.common.net.MediaType;
import com.google.gerrit.httpd.restapi.RestApiServlet;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class HealthCheckServlet extends HttpServlet {

  private final HealthCheck reviewDbCheck;
  private final Gson gson;

  @Inject
  public HealthCheckServlet(@Named(REVIEWDB) HealthCheck reviewDbCheck, Gson gson) {
    this.reviewDbCheck = reviewDbCheck;
    this.gson = gson;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try (PrintWriter out = resp.getWriter()) {
      CheckStatus checkStatus = doCheck();
      resp.setHeader("Content-Type", MediaType.JSON_UTF_8.toString());
      resp.setStatus(HttpServletResponse.SC_OK);
      out.print(new String(RestApiServlet.JSON_MAGIC, StandardCharsets.UTF_8));
      gson.toJson(checkStatus, out);
    }
  }

  private CheckStatus doCheck() {
    CheckStatus checkStatus = new CheckStatus();
    checkStatus.reviewdb = reviewDbCheck.check();
    return checkStatus;
  }
}
