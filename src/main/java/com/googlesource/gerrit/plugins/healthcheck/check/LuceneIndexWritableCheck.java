package com.googlesource.gerrit.plugins.healthcheck.check;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.index.change.ChangeIndexer;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.LUCENEINDEXWRITABLE;

public class LuceneIndexWritableCheck extends AbstractHealthCheck {

  private final ChangeIndexer changeIndexer;

  @Inject
  public LuceneIndexWritableCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      ChangeIndexer changeIndexer) {
    super(executor, config, LUCENEINDEXWRITABLE, healthCheckMetricsFactory);
    this.changeIndexer = changeIndexer;
  }


  @Override
  protected Result doCheck() throws Exception {

    try {
      changeIndexer.index(Project.NameKey.parse("test"), Change.id(1));
    } catch (StorageException e) {
      return Result.FAILED;
    }
    return Result.PASSED;
  }
}
