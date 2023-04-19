package com.googlesource.gerrit.plugins.healthcheck.check;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.server.config.SitePaths;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.WRITETOINDEX;

public class WriteToIndexCheck extends AbstractHealthCheck {

  private static final Logger log = LoggerFactory.getLogger(WriteToIndexCheck.class);
  private static final String WRITE_LOCK_FILE_NAME = "write.lock";
  private static final String CHANGES_DIR_NAME = "changes";
  private static final String OPEN_DIR = "/open";
  private static final String CLOSED_DIR = "/closed";
  private final Path absolutePathToIndex;


  @Inject
  public WriteToIndexCheck(
      ListeningExecutorService executor,
      HealthCheckConfig config,
      HealthCheckMetrics.Factory healthCheckMetricsFactory,
      SitePaths sitePaths) {
    super(executor, config, WRITETOINDEX, healthCheckMetricsFactory);
    this.absolutePathToIndex = sitePaths.index_dir.resolve("").toAbsolutePath();
  }

  @Override
  protected Result doCheck() throws Exception {
    return checkIfContainsFile(getPossibleDirectories()) ? Result.PASSED : Result.FAILED;
  }

  private boolean checkIfContainsFile(Set<String> paths) {
    Set<String> updatedPaths = addSubDirectories(paths);

    for (String path : updatedPaths) {
      File directory = new File(path);
      File file = new File(directory, WRITE_LOCK_FILE_NAME);

      if (!file.exists()) {
        return false;
      }
    }
    return true;
  }

  private Set<String> getPossibleDirectories() throws IOException {
    return Files.list(absolutePathToIndex)
        .filter(file -> !file.endsWith("gerrit_index.config"))
        .map(path -> path.toString())
        .collect(Collectors.toSet());
  }

  private Set<String> addSubDirectories(Set<String> paths) {
//    Set<String> updatedPaths = paths;
    for (String path : paths) {
      if (path.contains(CHANGES_DIR_NAME)) {
        paths.remove(path);
        paths.add(path + CLOSED_DIR);
        paths.add(path + OPEN_DIR);
        break;
      }
    }
    return paths;
  }

}
