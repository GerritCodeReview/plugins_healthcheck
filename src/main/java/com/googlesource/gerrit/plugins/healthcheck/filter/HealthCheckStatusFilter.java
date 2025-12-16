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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.httpd.restapi.RestApiServlet;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.ExceptionHook;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckExceptionHook;
import com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.lib.Config;

public class HealthCheckStatusFilter extends AllRequestFilter {
  public static final Gson GSON = OutputFormat.JSON.newGsonBuilder().create();

  private final HealthCheckStatusEndpoint statusEndpoint;
  private final String pluginName;
  private final Config cfg;
  private final String uriPattern;
  private final HealthCheckExceptionHook exceptionHook;

  @Inject
  public HealthCheckStatusFilter(
      HealthCheckStatusEndpoint statusEndpoint,
      @PluginName String pluginName,
      @GerritServerConfig Config cfg,
      HealthCheckExceptionHook exceptionHook) {
    this.statusEndpoint = statusEndpoint;
    this.pluginName = pluginName;
    this.cfg = cfg;
    this.uriPattern = getUriPattern();
    this.exceptionHook = exceptionHook;
  }

  private static List<String> extractUriPrefixes(String[] listenUrls) {
    List<String> prefixes = new ArrayList<>();
    if (listenUrls.length == 0) {
      return prefixes;
    }
    for (String listenUrl : listenUrls) {
      String[] parts = listenUrl.split("/", 4);
      if (parts.length < 4) {
        continue;
      }
      String uriPrefix = parts[3];
      if (uriPrefix.endsWith("/")) {
        uriPrefix = uriPrefix.substring(0, uriPrefix.length() - 1);
      }
      prefixes.add(uriPrefix);
    }
    return prefixes;
  }

  private String getUriPattern() {
    List<String> uriPrefixes = extractUriPrefixes(cfg.getStringList("httpd", null, "listenUrl"));
    String prefixPattern = "";
    if (uriPrefixes.size() == 1 && !uriPrefixes.get(0).isEmpty()) {
      prefixPattern = "(?:/" + uriPrefixes.get(0) + ")";
    } else if (uriPrefixes.size() > 1) {
      prefixPattern = "(?:/";
      for (String prefix : uriPrefixes) {
        if (!prefix.isEmpty()) {
          prefixPattern += prefix + "|";
        }
      }
      prefixPattern = prefixPattern.substring(0, prefixPattern.length() - 1);
      prefixPattern += ")";
      if (uriPrefixes.contains("")) {
        prefixPattern += "?";
      }
    }
    return prefixPattern + "(?:/a)?/config/server/" + pluginName + "~status";
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

  @VisibleForTesting
  protected boolean isStatusCheck(HttpServletRequest httpServletRequest) {
    return httpServletRequest.getRequestURI().matches(uriPattern);
  }

  private void doStatusCheck(HttpServletResponse httpResponse)
      throws ServletException, IOException {
    try {
      Response<Map<String, Object>> healthStatus = statusEndpoint.apply(new ConfigResource());
      String healthStatusJson = GSON.toJson(healthStatus.value());
      if (healthStatus.statusCode() == HttpServletResponse.SC_OK) {
        PrintWriter writer = httpResponse.getWriter();
        writer.print(new String(RestApiServlet.JSON_MAGIC));
        writer.print(healthStatusJson);
      } else {
        httpResponse.sendError(healthStatus.statusCode(), healthStatusJson);
      }
    } catch (Exception e) {
      Optional<ExceptionHook.Status> status = exceptionHook.getStatus(e);
      if (status.isPresent()) {
        httpResponse.sendError(status.get().statusCode(), status.get().statusMessage());
      } else {
        throw new ServletException(e);
      }
    }
  }
}
