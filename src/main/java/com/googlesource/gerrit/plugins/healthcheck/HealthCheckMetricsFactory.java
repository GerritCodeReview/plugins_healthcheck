package com.googlesource.gerrit.plugins.healthcheck;

public interface HealthCheckMetricsFactory {
  HealthCheckMetrics create(String name);
}
