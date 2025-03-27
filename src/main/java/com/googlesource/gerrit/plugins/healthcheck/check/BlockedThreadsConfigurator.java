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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.healthcheck.HealthCheckConfig;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck.Collector;
import com.googlesource.gerrit.plugins.healthcheck.check.BlockedThreadsCheck.CollectorProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@VisibleForTesting
public class BlockedThreadsConfigurator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Pattern THRESHOLD_PATTERN = Pattern.compile("^(\\d\\d?)$");

  static final int DEFAULT_BLOCKED_THREADS_THRESHOLD = 50;

  private final List<CollectorProvider<?>> providers;

  @Inject
  BlockedThreadsConfigurator(
      BlockedThreadsSubCheck.Factory subchecks, HealthCheckConfig healthCheckConfig) {
    this.providers = getProviders(subchecks, healthCheckConfig);
  }

  List<Collector> createCollectors() {
    return providers.stream().map(CollectorProvider::get).collect(toList());
  }

  private static List<CollectorProvider<?>> getProviders(
      BlockedThreadsSubCheck.Factory subchecksFactory, HealthCheckConfig healthCheckConfig) {
    return getConfig(healthCheckConfig.getListOfBlockedThreadsThresholds()).stream()
        .map(spec -> collectorProvider(subchecksFactory, spec))
        .collect(toList());
  }

  private static CollectorProvider<?> collectorProvider(
      BlockedThreadsSubCheck.Factory subchecksFactory, Threshold spec) {
    return spec.prefix.isPresent()
        ? subchecksFactory.create(spec.prefix.get(), spec.value)
        : () -> new BlockedThreadsCheck.Collector(spec.value);
  }

  @VisibleForTesting
  static Collection<Threshold> getConfig(String[] thresholds) {
    // Threshold can be defined as a sole value e.g
    //  threshold = 80
    // and would become a default one for all blocked threads check or as a set of specific thread
    // groups checks defined like
    //  threshold = foo=30
    //  threshold = bar=40
    //  ...
    // they are mutually exclusive which means that one either checks all threads or groups
    Map<Boolean, List<Threshold>> specsClassified =
        Arrays.stream(thresholds)
            .filter(spec -> !spec.isEmpty())
            .map(BlockedThreadsConfigurator::getSpec)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(groupingBy(Threshold::hasPrefix));

    // check configuration consistency
    if (specsClassified.size() > 1) {
      Collection<Threshold> specs = deduplicatePrefixes(specsClassified.get(true));
      logger.atWarning().log(
          "Global and specific thresholds were configured for blocked threads check. Specific"
              + " configuration is used %s.",
          specs.stream().map(Threshold::toString).collect(Collectors.joining(", ")));
      return specs;
    }

    if (specsClassified.size() == 1) {
      Map.Entry<Boolean, List<Threshold>> entry = specsClassified.entrySet().iterator().next();
      return Boolean.TRUE == entry.getKey()
          ? deduplicatePrefixes(entry.getValue())
          : deduplicateGlobal(entry.getValue());
    }

    logger.atInfo().log(
        "Default blocked threads check is configured with %d%% threshold",
        DEFAULT_BLOCKED_THREADS_THRESHOLD);
    return ImmutableSet.of(new Threshold(DEFAULT_BLOCKED_THREADS_THRESHOLD));
  }

  private static Collection<Threshold> deduplicateGlobal(List<Threshold> input) {
    if (input.size() > 1) {
      Threshold spec = input.get(input.size() - 1);
      logger.atWarning().log("Multiple threshold values were configured. Using %s", spec);
      return ImmutableSet.of(spec);
    }
    return input;
  }

  private static Collection<Threshold> deduplicatePrefixes(Collection<Threshold> input) {
    Map<String, Threshold> deduplicated = new HashMap<>();
    input.forEach(t -> deduplicated.put(t.prefix.get(), t));
    if (deduplicated.size() != input.size()) {
      logger.atWarning().log(
          "The same prefixes were configured multiple times. The following configuration is used"
              + " %s",
          deduplicated.values().stream()
              .map(Threshold::toString)
              .collect(Collectors.joining(", ")));
    }
    return deduplicated.values();
  }

  private static Optional<Threshold> getSpec(String spec) {
    int equals = spec.lastIndexOf('=');
    if (equals != -1) {
      Optional<Integer> maybeThreshold = isThresholdDefined(spec.substring(equals + 1));
      if (maybeThreshold.isPresent()) {
        return Optional.of(new Threshold(spec.substring(0, equals).trim(), maybeThreshold.get()));
      }
    } else {
      Optional<Integer> maybeThreshold = isThresholdDefined(spec);
      if (maybeThreshold.isPresent()) {
        return Optional.of(new Threshold(maybeThreshold.get()));
      }
    }

    logger.atWarning().log("Invalid configuration of blocked threads threshold [%s]", spec);
    return Optional.empty();
  }

  private static Optional<Integer> isThresholdDefined(String input) {
    Matcher value = THRESHOLD_PATTERN.matcher(input.trim());
    if (value.matches()) {
      return Optional.of(Integer.valueOf(value.group(1)));
    }
    return Optional.empty();
  }

  @VisibleForTesting
  static class Threshold {
    final Optional<String> prefix;
    final Integer value;

    Threshold(int value) {
      this(null, value);
    }

    Threshold(String prefix, int value) {
      this.prefix = Optional.ofNullable(prefix);
      this.value = value;
    }

    boolean hasPrefix() {
      return prefix.isPresent();
    }

    @Override
    public int hashCode() {
      return Objects.hash(prefix, value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Threshold other = (Threshold) obj;
      return Objects.equals(prefix, other.prefix) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
      return new StringBuilder()
          .append("Threshold [prefix=")
          .append(prefix)
          .append(", value=")
          .append(value)
          .append("]")
          .toString();
    }
  }
}
