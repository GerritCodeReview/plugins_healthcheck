// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.healthcheck;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig.DEFAULT_CONFIG;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.config.ThreadSettingsConfig;
import com.google.gerrit.server.index.change.ChangeSchemaDefinitions;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.healthcheck.check.ActiveWorkersCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.LuceneIndexWritableCheck;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;

@UseLocalDisk
public class LuceneIndexWritableCheckTest {
  //  private AllProjectsName allProjectsName = new AllProjectsName("All-Projects");
  //  private AllUsersName allUsersName = new AllUsersName("All-Users");
  //  private InMemoryRepositoryManager inMemoryRepositoryManager = new InMemoryRepositoryManager();
  //  private PersonIdent personIdent = new PersonIdent("Gerrit Rietveld", "gerrit@rietveld.nl");

  private static Path random() throws IOException {
    Path tmp = Files.createTempFile("gerrit_test_", "_site");
    Files.deleteIfExists(tmp);
    return tmp;
  }

  @Inject private ListeningExecutorService executor;
  //  @Inject private SitePaths sitePaths;
  HealthCheckMetrics.Factory healthCheckMetricsFactory = new DummyHealthCheckMetricsFactory();

  @Before
  public void setupAllProjects() throws Exception {
    Guice.createInjector(new HealthCheckModule()).injectMembers(this);

    //    InMemoryRepositoryManager.Repo allProjects =
    //        inMemoryRepositoryManager.createRepository(allProjectsName);
    //    createCommit(allProjects, "refs/meta/config");
    //
    //    InMemoryRepositoryManager.Repo allUsers =
    //        inMemoryRepositoryManager.createRepository(allUsersName);
    //    createCommit(allUsers, "refs/meta/config");
  }

  @Test
  public void shouldPassWhenFoo() throws IOException {
    //    ImmutableList.of(
    //        AccountSchemaDefinitions.INSTANCE,
    //        ChangeSchemaDefinitions.INSTANCE,
    //        GroupSchemaDefinitions.INSTANCE,
    //        ProjectSchemaDefinitions.INSTANCE);
    ImmutableSortedSet<Integer> schemas = ChangeSchemaDefinitions.INSTANCE.getSchemas().keySet();
    System.out.println(schemas.first());
    Path root = random();
    Path lockFile = root.resolve("write.lock");
    try {
      Files.createDirectory(root);
      Files.createFile(lockFile);
      final SitePaths site = new SitePaths(random());
      LuceneIndexWritableCheck check =
          new LuceneIndexWritableCheck(executor, DEFAULT_CONFIG, healthCheckMetricsFactory, site);
      assertThat(check.run().result).isEqualTo(Result.PASSED);
    } finally {
      Files.delete(lockFile);
      Files.delete(root);
    }
  }

  private ActiveWorkersCheck createCheck(Injector injector, HealthCheckConfig healtchCheckConfig) {
    return new ActiveWorkersCheck(
        new Config(),
        injector.getInstance(ListeningExecutorService.class),
        healtchCheckConfig,
        injector.getInstance(ThreadSettingsConfig.class),
        injector.getInstance(MetricRegistry.class),
        healthCheckMetricsFactory);
  }
  //  @Test
  //  public void shouldBeHealthyWhenJGitIsWorking() {
  //    JGitHealthCheck check =
  //        new JGitHealthCheck(
  //            executor, DEFAULT_CONFIG, getWorkingRepositoryManager(), healthCheckMetricsFactory);
  //    assertThat(check.run().result).isEqualTo(Result.PASSED);
  //  }

  //  @Test
  //  public void shouldBeUnhealthyWhenJGitIsFailingForAllRepos() {
  //    JGitHealthCheck jGitHealthCheck =
  //        new JGitHealthCheck(
  //            executor, DEFAULT_CONFIG, getFailingGitRepositoryManager(),
  // healthCheckMetricsFactory);
  //    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.FAILED);
  //  }
  //
  //  @Test
  //  public void shouldBeUnhealthyWhenJGitIsFailingSomeRepos() {
  //    HealthCheckConfig config =
  //        new HealthCheckConfig(
  //            "[healthcheck \""
  //                + JGIT
  //                + "\"]\n"
  //                + "  project = All-Users\n"
  //                + "  project = Not-Existing-Repo");
  //    JGitHealthCheck jGitHealthCheck =
  //        new JGitHealthCheck(
  //            executor, config, getWorkingRepositoryManager(), healthCheckMetricsFactory);
  //    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.FAILED);
  //  }

  //  private GitRepositoryManager getFailingGitRepositoryManager() {
  //    return new GitRepositoryManager() {
  //
  //      @Override
  //      public Status getRepositoryStatus(Project.NameKey name) {
  //        return Status.ACTIVE;
  //      }
  //
  //      @Override
  //      public Repository openRepository(Project.NameKey name)
  //          throws RepositoryNotFoundException, IOException {
  //        throw new RepositoryNotFoundException("Can't find repository " + name);
  //      }
  //
  //      @Override
  //      public Repository createRepository(Project.NameKey name)
  //          throws RepositoryCaseMismatchException, RepositoryNotFoundException, IOException {
  //        throw new IOException("Can't create repositories");
  //      }
  //
  //      @Override
  //      public NavigableSet<Project.NameKey> list() {
  //        return Collections.emptyNavigableSet();
  //      }
  //    };
  //  }
  //
  //  private GitRepositoryManager getWorkingRepositoryManager() {
  //    return inMemoryRepositoryManager;
  //  }
  //
  //  private void createCommit(InMemoryRepositoryManager.Repo repo, String ref) throws IOException
  // {
  //    try (ObjectInserter oi = repo.newObjectInserter()) {
  //      CommitBuilder cb = new CommitBuilder();
  //      cb.setTreeId(oi.insert(Constants.OBJ_TREE, new byte[] {}));
  //      cb.setAuthor(personIdent);
  //      cb.setCommitter(personIdent);
  //      cb.setMessage("Test commit\n");
  //      ObjectId id = oi.insert(cb);
  //      oi.flush();
  //      RefUpdate ru = repo.updateRef(ref);
  //      ru.setNewObjectId(id);
  //      assertThat(ru.update()).isEqualTo(NEW);
  //    }
  //  }
}
