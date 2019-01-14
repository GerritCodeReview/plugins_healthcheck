load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:junit.bzl", "junit_tests")

gerrit_plugin(
    name = "healthcheck",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: healthcheck",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.healthcheck.HttpModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "healthcheck_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        ":healthcheck__plugin_test_deps",
    ],
)

java_library(
    name = "healthcheck__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":healthcheck__plugin",
    ],
)
