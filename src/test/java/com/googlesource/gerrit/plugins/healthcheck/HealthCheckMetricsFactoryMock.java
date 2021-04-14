package com.googlesource.gerrit.plugins.healthcheck;

import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import org.junit.Ignore;

@Ignore
public class HealthCheckMetricsFactoryMock implements HealthCheckMetricsFactory {
  @Override
  public HealthCheckMetrics create(String name) {
    MetricMaker metricMaker = new DisabledMetricMaker();
    return new HealthCheckMetrics(metricMaker, name);
  }
}
