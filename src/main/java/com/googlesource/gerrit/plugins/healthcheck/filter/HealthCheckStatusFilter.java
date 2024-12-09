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

package com.googlesource.gerrit.plugins.healthcheck.filter;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.httpd.restapi.RestApiServlet;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.lib.Config;

public class HealthCheckStatusFilter extends AllRequestFilter {
  private final HealthCheckStatusEndpoint statusEndpoint;
  private final Gson gson;
  private final String pluginName;
  private final String uriPrefix;

  @Inject
  public HealthCheckStatusFilter(
      HealthCheckStatusEndpoint statusEndpoint,
      @PluginName String pluginName,
      @GerritServerConfig Config cfg) {
    this.statusEndpoint = statusEndpoint;
    this.gson = OutputFormat.JSON.newGsonBuilder().create();
    this.pluginName = pluginName;
    this.uriPrefix = extractUriPrefix(cfg.getString("httpd", null, "listenUrl"));
  }

  private static String extractUriPrefix(String listenUrl) {
    String[] parts = listenUrl.split("/", 4);
    if (parts.length < 4) {
      return "";
    }
    String uriPrefix = parts[3];
    if (uriPrefix.endsWith("/")) {
      uriPrefix = uriPrefix.substring(0, uriPrefix.length() - 1);
    }
    return uriPrefix;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    if (isStatusCheck(httpRequest)) {
      doStatusCheck(httpResponse);
    } else {
      chain.doFilter(request, response);
    }
  }

  private boolean isStatusCheck(HttpServletRequest httpServletRequest) {
    String uriPattern = "(?:/a)?/config/server/" + pluginName + "~status";
    if (uriPrefix != null && !uriPrefix.isBlank()) {
      uriPattern = "(?:/" + uriPrefix + ")" + uriPattern;
    }
    return httpServletRequest.getRequestURI().matches(uriPattern);
  }

  private void doStatusCheck(HttpServletResponse httpResponse) throws ServletException {
    try {
      Response<Map<String, Object>> healthStatus = statusEndpoint.apply(new ConfigResource());
      String healthStatusJson = gson.toJson(healthStatus.value());
      if (healthStatus.statusCode() == HttpServletResponse.SC_OK) {
        PrintWriter writer = httpResponse.getWriter();
        writer.print(new String(RestApiServlet.JSON_MAGIC));
        writer.print(healthStatusJson);
      } else {
        httpResponse.sendError(healthStatus.statusCode(), healthStatusJson);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}
