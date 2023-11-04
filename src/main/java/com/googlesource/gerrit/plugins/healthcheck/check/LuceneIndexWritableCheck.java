package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.FAILED;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result.PASSED;
import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.LUCENEINDEXWRITABLE;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class LuceneIndexWritableCheck extends AbstractHealthCheck {

  //  private final LuceneAccountIndex luceneAccountIndex;
  private final SitePaths sitePaths;
  //  private final Schema<AccountState> accountStateSchema;
  private static final String ACCOUNTS = "accounts";
  private static final String CHANGES = "changes";
  private static final String GROUPS = "groups";
  private static final String PROJECTS = "projects";

  @Inject
  public LuceneIndexWritableCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
//      String name,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      //      LuceneAccountIndex luceneAccountIndex,
      SitePaths sitePaths
      //      @Assisted Schema<AccountState> accountStateSchema) {
      ) {
    super(executor, config, LUCENEINDEXWRITABLE, healthCheckMetricsFactory);
    //    this.luceneAccountIndex = luceneAccountIndex;
    this.sitePaths = sitePaths;
    //    this.accountStateSchema = accountStateSchema;
  }

  @Override
  protected Result doCheck() throws Exception {
    List<Path> indexDirs =
        Arrays.asList(
            getDir(sitePaths, ACCOUNTS, 11),
            getDir(sitePaths, GROUPS, 8),
            getDir(sitePaths, PROJECTS, 4),
            getDir(sitePaths, CHANGES, 77, Optional.of("/open")),
            getDir(sitePaths, CHANGES, 77, Optional.of("/closed")));
    if (indexDirs.stream().allMatch(this::checkIndex)) {
      return PASSED;
    }
    return FAILED;
  }

  private boolean checkIndex(Path indexDir) {
    File lockFile = indexDir.resolve("write.lock").toFile();
    System.out.println(lockFile);
    return lockFile.exists() && lockFile.canWrite();
  }

  private static Path getDir(SitePaths sitePaths, String name, int version) {
    return getDir(sitePaths, name, version, Optional.empty());
  }

  private static Path getDir(
      SitePaths sitePaths, String accountName, int version, Optional<String> subdir) {
    //    return sitePaths.index_dir.resolve(String.format("%s_%04d", name, schema.getVersion()));
    return sitePaths.index_dir.resolve(
        String.format("%s_%04d%s", accountName, version, subdir.orElse("")));
  }
}
