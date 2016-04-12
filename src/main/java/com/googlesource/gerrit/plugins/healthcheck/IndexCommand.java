// Copyright (C) 2016 The Android Open Source Project
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

import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.CapabilityScope;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

@RequiresCapability(value = GlobalCapability.MAINTAIN_SERVER,
    scope = CapabilityScope.CORE)
@CommandMetaData(name = "index",
    description = "Reindex specific projects or changes")
final class IndexCommand extends SshCommand {
  @Option(name = "--all", usage = "index all known projects")
  private boolean all;

  @Option(name = "--change", metaVar = "CHANGE", usage = "changes to index")
  private List<String> change;

  @Argument(index = 0, multiValued = true, metaVar = "PROJECT",
      usage = "project name pattern")
  private List<String> projects = new ArrayList<>(2);

  private final ProjectCache projectCache;

  private final Changes changes;

  private final GerritApi api;

  @Inject
  public IndexCommand(ProjectCache projectCache, Changes changes,
      GerritApi api) {
    this.projectCache = projectCache;
    this.changes = changes;
    this.api = api;
  }

  @Override
  protected void run() throws UnloggedFailure, Failure, Exception {
    if (all && projects.size() > 0) {
      throw new UnloggedFailure(1, "error: cannot combine --all and PROJECT");
    }

    if ((all || projects.size() > 0) && change != null && change.size() > 0) {
      throw new UnloggedFailure(1,
          "error: cannot combine --all or PROJECT with --change");
    }

    if (all) {
      for (NameKey project : projectCache.all()) {
        projects.add(project.get());
      }
    }

    int start = 0;
    final int changeLimit = 1;
    int count = 0;

    List<ChangeInfo> changeList;
    if (change == null || change.size() == 0) {
      for (String project : projects) {
        while ((changeList =
            changes
                .query(
                    "(status:open OR NOT status:open) AND project:" + project)
                .withLimit(changeLimit).withStart(start).get()).size() > 0) {
          start += changeList.size();
          for (ChangeInfo changeInfo : changeList) {
            indexChange(changeInfo.id);
          }
          synchronized (stdout) {
            count += changeList.size();
            stdout.print("\rIndexed " + count + " changes...");
          }
        }
      }
    } else {
      for (int i = 0; i < change.size(); i++) {
        indexChange(change.get(i));
        synchronized (stdout) {
          count++;
          stdout.print("\rIndexed " + count + " changes...");
        }
      }
    }
    synchronized (stdout) {
      stdout.println("\rIndexed " + count + " changes... DONE");
    }
  }

  private void indexChange(String id) {
    try {
      api.changes().id(id).index();
    } catch (RestApiException e) {
      synchronized (stdout) {
        stdout.println("Failed to index change: " + e.getLocalizedMessage());
      }
    }
  }

}
