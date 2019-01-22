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
import static org.eclipse.jgit.lib.RefUpdate.Result.NEW;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.RepositoryCaseMismatchException;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.JGitHealthCheck;
import com.googlesource.gerrit.plugins.healthcheck.check.MetricsHandler;
import java.io.IOException;
import java.util.Collections;
import java.util.SortedSet;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class JGitHealthCheckTest {
  private AllProjectsName allProjectsName = new AllProjectsName("All-Projects");
  private InMemoryRepositoryManager inMemoryRepositoryManager = new InMemoryRepositoryManager();
  private PersonIdent personIdent = new PersonIdent("Gerrit Rietveld", "gerrit@rietveld.nl");
  private final MetricsHandler.Factory metricsHandlerFactory =
      new MetricsHandler.Factory() {
        @Override
        public MetricsHandler create(String name) {
          return new MetricsHandler("foo", new DisabledMetricMaker());
        }
      };

  @Inject private ListeningExecutorService executor;

  @Before
  public void setupAllProjects() throws Exception {
    Guice.createInjector(new HealthCheckModule()).injectMembers(this);

    InMemoryRepositoryManager.Repo repo =
        inMemoryRepositoryManager.createRepository(allProjectsName);
    createCommit(repo, "refs/meta/config");
  }

  @Test
  public void shouldBeHealthyWhenJGitIsWorking() {
    JGitHealthCheck reviewDbCheck =
        new JGitHealthCheck(
            executor,
            DEFAULT_CONFIG,
            getWorkingRepositoryManager(),
            allProjectsName,
            metricsHandlerFactory);
    assertThat(reviewDbCheck.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldBeUnhealthyWhenJGitIsFailing() {
    JGitHealthCheck jGitHealthCheck =
        new JGitHealthCheck(
            executor,
            DEFAULT_CONFIG,
            getFailingGitRepositoryManager(),
            allProjectsName,
            metricsHandlerFactory);
    assertThat(jGitHealthCheck.run().result).isEqualTo(Result.FAILED);
  }

  private GitRepositoryManager getFailingGitRepositoryManager() {
    return new GitRepositoryManager() {

      @Override
      public Repository openRepository(Project.NameKey name)
          throws RepositoryNotFoundException, IOException {
        throw new RepositoryNotFoundException("Can't fine repository " + name);
      }

      @Override
      public Repository createRepository(Project.NameKey name)
          throws RepositoryCaseMismatchException, RepositoryNotFoundException, IOException {
        throw new IOException("Can't create repositories");
      }

      @Override
      public SortedSet<Project.NameKey> list() {
        return Collections.emptySortedSet();
      }
    };
  }

  private GitRepositoryManager getWorkingRepositoryManager() {
    return inMemoryRepositoryManager;
  }

  private void createCommit(InMemoryRepositoryManager.Repo repo, String ref) throws IOException {
    try (ObjectInserter oi = repo.newObjectInserter()) {
      CommitBuilder cb = new CommitBuilder();
      cb.setTreeId(oi.insert(Constants.OBJ_TREE, new byte[] {}));
      cb.setAuthor(personIdent);
      cb.setCommitter(personIdent);
      cb.setMessage("Test commit\n");
      ObjectId id = oi.insert(cb);
      oi.flush();
      RefUpdate ru = repo.updateRef(ref);
      ru.setNewObjectId(id);
      assertThat(ru.update()).isEqualTo(NEW);
    }
  }
}
