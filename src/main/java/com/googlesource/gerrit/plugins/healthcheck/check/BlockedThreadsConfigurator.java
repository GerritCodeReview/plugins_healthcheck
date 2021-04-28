// Copyright (C) 2021 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.healthcheck.check;

import static com.googlesource.gerrit.plugins.healthcheck.check.HealthCheckNames.BLOCKEDTHREADS;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck.Collector;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck.CollectorProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockedThreadsConfigurator {
  private static final Logger log = LoggerFactory.getLogger(BlockedThreadsConfigurator.class);
  private static final Pattern THRESHOLD_PATTERN = Pattern.compile("^(\\d\\d?)%?$");

  static final int DEFAULT_BLOCKED_THREADS_THRESHOLD = 50;

  private final List<CollectorProvider<?>> providers;

  @Inject
  BlockedThreadsConfigurator(
      BlockedThreadsSubCheck.Factory subchecks, HealthCheckConfig healthCheckConfig) {
    this.providers = getProviders(subchecks, healthCheckConfig);
  }

  List<Collector> collectors() {
    return providers.stream().map(CollectorProvider::get).collect(toList());
  }

  private static List<CollectorProvider<?>> getProviders(
      BlockedThreadsSubCheck.Factory subchecks, HealthCheckConfig healthCheckConfig) {
    String[] specs = healthCheckConfig.getThresholds(BLOCKEDTHREADS);
    if (specs.length == 0) {
      log.info(
          "Default blocked threads check is configured with {}% threshold",
          DEFAULT_BLOCKED_THREADS_THRESHOLD);
      return ImmutableList.of(() -> defaultCollector(DEFAULT_BLOCKED_THREADS_THRESHOLD));
    }

    Config config = getConfig(specs);
    if (config.specs.isEmpty()) {
      log.info("Default blocked threads check is configured with {}% threshold", config.threshold);
      return ImmutableList.of(() -> defaultCollector(config.threshold));
    }

    return config.specs.stream()
        .map(s -> subchecks.create(s.prefix, s.value.orElse(config.threshold)))
        .collect(toList());
  }

  private static BlockedThreadsCheck.Collector defaultCollector(int threshold) {
    return new BlockedThreadsCheck.Collector(threshold);
  }

  @VisibleForTesting
  static Config getConfig(String[] thresholds) {
    // Threshold can be defined as a sole value e.g
    //  threshold = 80
    // and would become a default one for all specific blocked thread checks
    // In terms of specific check one could define it like
    //  threshold = SSH-Interactive-Worker=30
    // which would mean that for the given prefix 30% threshold should be used
    // or like that
    //  threshold = SSH-Interactive-Worker
    // which would mean that for given prefix default (or redefined in the first case) value should
    // be used
    int threshold = DEFAULT_BLOCKED_THREADS_THRESHOLD;
    List<Threshold> specs = new ArrayList<>();
    for (String spec : thresholds) {
      spec = spec.trim();
      if (spec.isEmpty()) {
        continue;
      }

      int equals = spec.lastIndexOf('=');
      if (equals != -1) {
        Matcher value = THRESHOLD_PATTERN.matcher(spec.substring(equals + 1).trim());
        if (!value.matches()) {
          log.warn("Blocked threads spec [threshold={}] is invalid", spec);
          continue;
        }
        specs.add(new Threshold(spec.substring(0, equals).trim(), Integer.valueOf(value.group(1))));
      } else {
        Matcher value = THRESHOLD_PATTERN.matcher(spec.trim());
        if (value.matches()) {
          threshold = Integer.valueOf(value.group(1));
        } else {
          specs.add(new Threshold(spec, null));
        }
      }
    }
    return new Config(threshold, specs);
  }

  @VisibleForTesting
  static class Threshold {
    final String prefix;
    final Optional<Integer> value;

    Threshold(String prefix, Integer value) {
      this.prefix = prefix;
      this.value = Optional.ofNullable(value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(prefix, value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Threshold other = (Threshold) obj;
      return Objects.equals(prefix, other.prefix) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder
          .append("Threshold [prefix=")
          .append(prefix)
          .append(", value=")
          .append(value)
          .append("]");
      return builder.toString();
    }
  }

  @VisibleForTesting
  static class Config {
    final int threshold;
    final List<Threshold> specs;

    Config(int threshold, List<Threshold> specs) {
      this.threshold = threshold;
      this.specs = specs;
    }
  }
}
