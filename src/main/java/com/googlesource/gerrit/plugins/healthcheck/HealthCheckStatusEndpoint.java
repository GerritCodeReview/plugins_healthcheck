package com.googlesource.gerrit.plugins.healthcheck;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class HealthCheckStatusEndpoint implements RestReadView<ConfigResource> {

  private final DynamicSet<HealthCheck> healthChecks;

  @Inject
  public HealthCheckStatusEndpoint(DynamicSet<HealthCheck> healthChecks) {
    this.healthChecks = healthChecks;
  }

  @Override
  public Object apply(ConfigResource resource)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {
    Iterable<HealthCheck> iterable = () -> healthChecks.iterator();
    return StreamSupport.stream(iterable.spliterator(), true)
        .map(check -> check.run())
        .collect(Collectors.toMap(r -> r.name, r -> r.result));
  }
}
