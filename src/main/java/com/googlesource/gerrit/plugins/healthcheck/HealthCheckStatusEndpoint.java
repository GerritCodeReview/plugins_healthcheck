package com.googlesource.gerrit.plugins.healthcheck;

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Singleton;

@Singleton
public class HealthCheckStatusEndpoint implements RestReadView<ConfigResource> {

  @Override
  public Object apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    return "{}";
  }
}
