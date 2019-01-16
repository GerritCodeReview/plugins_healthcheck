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
import static org.eclipse.jgit.lib.RefUpdate.Result.NEW;

import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.RepositoryCaseMismatchException;
import com.google.gerrit.testutil.InMemoryRepositoryManager;
import com.googlesource.gerrit.plugins.healthcheck.check.HealthCheck.Result;
import com.googlesource.gerrit.plugins.healthcheck.check.JGitHealthCheck;
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
  PersonIdent personIdent = new PersonIdent("Gerrit Rietveld", "gerrit@rietveld.nl");

  @Before
  public void setupAllProjects() throws Exception {
    InMemoryRepositoryManager.Repo repo =
        inMemoryRepositoryManager.createRepository(allProjectsName);
    createCommit(repo, "refs/meta/config");
  }

  @Test
  public void shouldBeHealthyWhenJGitIsWorking() {
    JGitHealthCheck reviewDbCheck =
        new JGitHealthCheck(getWorkingRepositoryManager(), allProjectsName);
    assertThat(reviewDbCheck.run().result).isEqualTo(Result.PASSED);
  }

  @Test
  public void shouldBeUnhealthyWhenJGitIsFailing() {
    JGitHealthCheck jGitHealthCheck =
        new JGitHealthCheck(getFailingGitRepositoryManager(), allProjectsName);
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
