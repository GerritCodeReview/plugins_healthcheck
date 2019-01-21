package com.googlesource.gerrit.plugins.healthcheck;

import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.Timer0;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Status;
import com.googlesource.gerrit.plugins.healthcheck.check.MetricsHandler;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class MetricsHandlerTest {

    private class TestMetricsMaker extends DisabledMetricMaker {
        private Long failures = 0L;
        private Long latency = 0L;

        @Override
        public Counter0 newCounter(String name, Description desc) {
          return new Counter0() {
            @Override
            public void incrementBy(long value) {
              failures += value;
            }

            @Override
            public void remove() {}
          };
        }

        @Override
        public Timer0 newTimer(String name, Description desc) {
            return new Timer0(name) {
                @Override
                protected void doRecord(long value, TimeUnit unit) {
                    latency = value;
                }

                @Override
                public void remove() {}
            };
        }
    }

    @Test
    public void shouldSendCounterWhenStatusFailed() {
        TestMetricsMaker metricMaker = new TestMetricsMaker();
        MetricsHandler handler = new MetricsHandler("test", metricMaker);

      handler.sendMetrics(new Status(Result.FAILED, 1L, 1L));

      assertThat(metricMaker.failures).isEqualTo(1L);
    }

    @Test
    public void shouldSendCounterWhenStatusTimeout() {
        TestMetricsMaker metricMaker = new TestMetricsMaker();
        MetricsHandler handler = new MetricsHandler("test", metricMaker);

        handler.sendMetrics(new Status(Result.TIMEOUT, 1L, 1L));

        assertThat(metricMaker.failures).isEqualTo(1L);
    }

    @Test
    public void shouldNOTSendCounterWhenStatusSuccess() {
        TestMetricsMaker metricMaker = new TestMetricsMaker();
        MetricsHandler handler = new MetricsHandler("test", metricMaker);

        handler.sendMetrics(new Status(Result.PASSED, 1L, 1L));

        assertThat(metricMaker.failures).isEqualTo(0L);
    }

    @Test
    public void shouldRecordLatencyWhenSuccess() {
        TestMetricsMaker metricMaker = new TestMetricsMaker();
        MetricsHandler handler = new MetricsHandler("test", metricMaker);
        Long elapsed = System.currentTimeMillis();

        handler.sendMetrics(new Status(Result.PASSED, 1L, elapsed));

        assertThat(metricMaker.latency).isEqualTo(elapsed);
    }

    @Test
    public void shouldRecordLatencyWhenFailure() {
        TestMetricsMaker metricMaker = new TestMetricsMaker();
        MetricsHandler handler = new MetricsHandler("test", metricMaker);
        Long elapsed = System.currentTimeMillis();

        handler.sendMetrics(new Status(Result.FAILED, 1L, elapsed));

        assertThat(metricMaker.latency).isEqualTo(elapsed);
    }
}
