package com.googlesource.gerrit.plugins.healthcheck.check;

import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.Histogram0;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;

import java.util.concurrent.TimeUnit;

public class Metrics {
    private final Timer0 latencyMetric;
    private final Histogram0 failureMetric;

    Metrics(String name, MetricMaker metricMaker) {
        this.latencyMetric = metricMaker.newTimer(name + "/latency",
                new Description("Latency of executing '" + name + "' healthcheck")
                        .setCumulative()
                        .setUnit(Description.Units.MILLISECONDS)
        );

        this.failureMetric = metricMaker.newHistogram(name + "/failure",
                new Description("failures for '" + name + "' healthcheck")
                        .setCumulative()
                        .setUnit(name + "healthcheck failures")
        );
    }

    void sendMetrics(HealthCheck.Status status) {
        latencyMetric.record(status.elapsed, TimeUnit.MILLISECONDS);
        failureMetric.record(status.isFailure() ? 1L : 0L);
    }
}
