load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")

PLUGIN = "healthcheck"

# Constants inlined from //tools/bzl:plugin.bzl to avoid legacy dependency.
PLUGIN_DEPS = ["//plugins:plugin-lib"]

PLUGIN_TEST_DEPS = [
    "//java/com/google/gerrit/acceptance:lib",
    "//lib/bouncycastle:bcpg",
    "//lib/bouncycastle:bcpkix",
    "//lib/bouncycastle:bcprov",
]

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: healthcheck",
        "Gerrit-Module: com.googlesource.gerrit.plugins.healthcheck.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.healthcheck.HttpModule",
        "Gerrit-ApiModule: com.googlesource.gerrit.plugins.healthcheck.HealthCheckExtensionApiModule",
    ],
    plugin = PLUGIN,
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_plugin_tests(
    name = "healthcheck_tests",
    srcs = glob(
        ["src/test/java/**/*Test.java"],
        exclude = ["src/test/java/**/Abstract*.java"],
    ),
    plugin = PLUGIN,
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        ":healthcheck_test_util",
        "//javatests/com/google/gerrit/util/http/testutil",
    ],
)

[junit_tests(
    name = f[:f.index(".")].replace("/", "_"),
    srcs = [f],
    tags = ["owners"],
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":healthcheck__plugin",
        ":healthcheck_test_util",
    ],
) for f in glob(["src/test/java/**/*IT.java"])]

java_library(
    name = "healthcheck_test_util",
    testonly = 1,
    srcs = glob(["src/test/java/**/Abstract*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":healthcheck__plugin",
    ],
)