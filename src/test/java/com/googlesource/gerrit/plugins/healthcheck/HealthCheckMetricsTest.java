package com.googlesource.gerrit.plugins.healthcheck;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Counter0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.healthcheck.check.AbstractHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Status;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class HealthCheckMetricsTest {

  @Inject
  private ListeningExecutorService executor;
  private TestMetrics testMetrics = new TestMetrics();

  @Before
  public void setUp() throws Exception {
    testMetrics.reset();
  }

  private void setWithStatus(Status status) {
    TestHealthCheck testHealthCheck = new TestHealthCheck(executor, "test", status);

    Injector injector =
            testInjector(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        DynamicSet.bind(binder(), HealthCheck.class).toInstance(testHealthCheck);
                        bind(MetricMaker.class).toInstance(testMetrics);
                        bind(LifecycleListener.class).to(HealthCheckMetrics.class);
                      }
                    });
    HealthCheckMetrics healthCheckMetrics = injector.getInstance(HealthCheckMetrics.class);

    healthCheckMetrics.start();
    testHealthCheck.run();
    healthCheckMetrics.triggerAll();
  }

  @Test
  public void shouldSendCounterWhenStatusFailed() {
    Long elapsed = 100L;
    setWithStatus(new Status(Result.FAILED, 1L, elapsed));

    assertThat(testMetrics.getFailures()).isEqualTo(1);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }


  @Test
  public void shouldSendCounterWhenStatusTimeout() {
    Long elapsed = 100L;
    setWithStatus(new Status(Result.TIMEOUT, 1L, elapsed));

    assertThat(testMetrics.getFailures()).isEqualTo(1);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }


  @Test
  public void shouldNOTSendCounterWhenStatusSuccess() {
    Long elapsed = 100L;
    setWithStatus(new Status(Result.PASSED, 1L, elapsed));

    assertThat(testMetrics.failures).isEqualTo(0L);
    assertThat(testMetrics.getLatency()).isEqualTo(elapsed);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }

  @Singleton
  private static class TestMetrics extends DisabledMetricMaker {
    private Long failures = 0L;
    private Long latency = 0L;

    public Long getFailures() { return failures; }
    public Long getLatency() { return latency; }

    public void reset() {
      failures = 0L;
      latency = 0L;
    }

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
    public <V> CallbackMetric0<V> newCallbackMetric(
            String name, Class<V> valueClass, Description desc) {
      return new CallbackMetric0<V>() {
        @Override
        public void set(V value) {
          latency = (Long) value;
        }

        @Override
        public void remove() {}
      };
    }
  }

  private static class TestHealthCheck extends AbstractHealthCheck {
    private final Status returnStatus;

    protected TestHealthCheck(ListeningExecutorService executor, String name, Status returnStatus) {
      super(executor, HealthCheckConfig.DEFAULT_CONFIG, name);
      this.returnStatus = returnStatus;
    }

    @Override
    protected Result doCheck() {
      return returnStatus.result;
    }

    @Override
    public Status run() {
      latestStatus = returnStatus;
      return latestStatus;
    }
  }
}
