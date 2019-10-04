package com.googlesource.gerrit.plugins.healthcheck.filter;

import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.httpd.AllRequestFilter;
import com.google.gerrit.httpd.restapi.RestApiServlet;
import com.google.gerrit.server.config.ConfigResource;
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

public class HealthCheckStatusFilter extends AllRequestFilter {
  private final HealthCheckStatusEndpoint statusEndpoint;
  private final Gson gson;

  @Inject
  public HealthCheckStatusFilter(HealthCheckStatusEndpoint statusEndpoint, Gson gson) {
    this.statusEndpoint = statusEndpoint;
    this.gson = gson;
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
    return httpServletRequest.getRequestURI().equals("/config/server/healthcheck~status");
  }

  private void doStatusCheck(HttpServletResponse httpResponse) throws ServletException {
    try {
      Response<Map<String, Object>> healthStatus =
          (Response<Map<String, Object>>) statusEndpoint.apply(new ConfigResource());
      String healthStatusJson = gson.toJson(healthStatus);
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
