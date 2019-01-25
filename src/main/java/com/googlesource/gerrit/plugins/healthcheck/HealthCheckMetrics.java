package com.googlesource.gerrit.plugins.healthcheck;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Singleton
public class HealthCheckMetrics implements LifecycleListener {

    private final DynamicSet<HealthCheck> healthChecks;
    private final MetricMaker metricMaker;
    private final Set<RegistrationHandle> registeredMetrics;
    private final Set<Runnable> triggers;

    @Inject
    public HealthCheckMetrics(DynamicSet<HealthCheck> healthChecks, MetricMaker metricMaker) {
        this.healthChecks = healthChecks;
        this.metricMaker = metricMaker;
        this.registeredMetrics = Collections.synchronizedSet(new HashSet<>());
        this.triggers = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void start() {

        for(HealthCheck healthCheck: healthChecks) {
            String name = healthCheck.name();

            Counter0 failureMetric =
                    metricMaker.newCounter(
                            String.format("%s/failure", name),
                            new Description(String.format("%s healthcheck failures count", name))
                                    .setCumulative()
                                    .setRate()
                                    .setUnit("failures"));

            CallbackMetric0 latencyMetric = metricMaker.newCallbackMetric(
                    String.format("%s/latency", name),
                    Long.class,
                    new Description(String.format("%s health check latency execution (ms)", name))
                            .setGauge()
                            .setUnit(Description.Units.MILLISECONDS)
            );

            Runnable metricCallBack = () -> {
                HealthCheck.Status status = healthCheck.getLatestStatus();
                latencyMetric.set(healthCheck.getLatestStatus().elapsed);
                if (status.isFailure()) {
                    failureMetric.increment();
                }
            };

            registeredMetrics.add(failureMetric);
            registeredMetrics.add(metricMaker.newTrigger(latencyMetric, metricCallBack));
            triggers.add(metricCallBack);
        }
    }

    @Override
    public void stop() {
        synchronized (registeredMetrics) {
            Iterator<RegistrationHandle> itr = registeredMetrics.iterator();
            while (itr.hasNext()) {
                itr.next().remove();
                itr.remove();
            }
        }
    }

    public void triggerAll() {
        synchronized (triggers) {
            triggers.forEach(Runnable::run);
        }
    }
}
