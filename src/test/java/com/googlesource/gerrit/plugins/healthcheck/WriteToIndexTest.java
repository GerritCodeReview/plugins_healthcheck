package com.googlesource.gerrit.plugins.healthcheck;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.WriteToIndexCheck;
import org.apache.sshd.common.file.util.BasePath;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig.DEFAULT_CONFIG;

public class WriteToIndexTest {

  HealthCheckMetrics.Factory healthCheckMetricsFactory = new DummyHealthCheckMetricsFactory();

  @Test
  public void shouldPassWhenFilesPresent() throws ConfigInvalidException, IOException {
    Injector injector = testInjector(new TestModule(createSitePaths()));
    WriteToIndexCheck writeToIndexCheck = createCheck(injector);

    assertThat(writeToIndexCheck.run().result).isEqualTo(HealthCheck.Result.PASSED);
  }

  private Injector testInjector(AbstractModule testModule) {
    return Guice.createInjector(new HealthCheckModule(), testModule);
  }
  private WriteToIndexCheck createCheck(Injector injector) throws ConfigInvalidException, IOException {
    return new WriteToIndexCheck(
        injector.getInstance(ListeningExecutorService.class),
        DEFAULT_CONFIG,
        healthCheckMetricsFactory,
        injector.getInstance(SitePaths.class)
    );
  }

  private SitePaths createSitePaths() throws IOException {
    return new SitePaths(Path.of(""));
  }

  private class TestModule extends AbstractModule {
    Config gerritConfig;
    SitePaths sitePaths;

    public TestModule(SitePaths sitePaths) {
      this.gerritConfig = new Config();
      this.sitePaths = sitePaths;

    }

    @Override
    protected void configure() {
      bind(Config.class).annotatedWith(GerritServerConfig.class).toInstance(gerritConfig);
    }
  }

}
