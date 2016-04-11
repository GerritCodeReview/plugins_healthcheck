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
import com.google.gerrit.extensions.api.changes.FixInput;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.extensions.annotations.CapabilityScope;
import com.google.gerrit.extensions.common.ProblemInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

@RequiresCapability(value = "administrateServer", scope = CapabilityScope.CORE)
@CommandMetaData(name = "fsck", description = "Check specific projects or changes")
final class FsckCommand extends SshCommand {
  @Option(name = "--all", usage = "check all known projects")
  private boolean all;

  @Option(name = "--fix", usage = "attempt fixing errors, may remove missing changes from DB")
  private boolean fix;

  @Option(name = "--change", metaVar = "CHANGE", usage = "changes to check")
  private String[] change;

  @Argument(index = 0, multiValued = true, metaVar = "PROJECT", usage = "project name pattern")
  private List<String> projects = new ArrayList<>(2);

  private final ProjectCache projectCache;

  private final Changes changes;

  private final GerritApi api;

  @Inject
  public FsckCommand(ProjectCache projectCache,
      Changes changes,
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

    if ((all || projects.size() > 0) && change != null && change.length > 0) {
      throw new UnloggedFailure(1, "error: cannot combine --all or PROJECT with --change");
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
    if (change == null || change.length == 0) {
      for (String project : projects) {
        while ((changeList = changes.query("(status:open OR NOT status:open) AND project:" + project).withLimit(changeLimit).withStart(start).get()).size() > 0) {
          start += changeList.size();
          for (ChangeInfo changeInfo : changeList) {
            checkChange(changeInfo.id, fix);
          }
          synchronized (stdout) {
            count += changeList.size();
            stdout.print("\rChecked " + count + " changes...");
          }
        }
      }
    } else {
      for (int i = 0; i < change.length; i++) {
        checkChange(change[i], fix);
        synchronized (stdout) {
          count++;
          stdout.print("\rChecked " + count + " changes...");
        }
      }
    }
    synchronized (stdout) {
      stdout.println("\rChecked " + count + " changes... DONE");
    }
  }

  private void checkChange(String id, boolean fix) {
    ChangeInfo info;
    try {
      if (fix) {
        FixInput fixInput = new FixInput();
        fixInput.deletePatchSetIfCommitMissing = true;
        info = api.changes().id(id).check(fixInput);
      } else {
        info = api.changes().id(id).check();
      }
      synchronized (stdout) {
        for (ProblemInfo problem : info.problems) {
          if (problem.status == ProblemInfo.Status.FIXED) {
            stdout.print("FIXED:  ");
          } else if (problem.status == ProblemInfo.Status.FIX_FAILED) {
            stdout.print("FAILED: ");
          }
          stdout.println(problem.message);
          if (!problem.outcome.isEmpty()) {
            stdout.println("\t" + problem.outcome);
          }
        }
      }
    } catch (RestApiException e) {
      synchronized (stdout) {
        stdout.println("Failed to check change: " + e.getLocalizedMessage());
      }
    }
  }

}