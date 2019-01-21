package com.googlesource.gerrit.plugins.healthcheck.check;

import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.gerrit.metrics.Timer0;

import java.util.concurrent.TimeUnit;

public class MetricsHandler {
    private final Timer0 latencyMetric;
    private final Counter0 failureMetric;

    public MetricsHandler(String name, MetricMaker metricMaker) {
        this.latencyMetric = metricMaker.newTimer(name + "/latency",
                new Description(String.format("%s health check latency execution (ms)", name))
                        .setCumulative()
                        .setUnit(Description.Units.MILLISECONDS)
        );

        this.failureMetric = metricMaker.newCounter(name + "/failure",
                new Description(String.format("%s healthcheck failures count", name))
                        .setCumulative()
                        .setRate()
                        .setUnit("failures")
        );
    }

    public void sendMetrics(HealthCheck.Status status) {
        latencyMetric.record(status.elapsed, TimeUnit.MILLISECONDS);
        if ( status.isFailure() ) {
            failureMetric.increment();
        }
    }
}
