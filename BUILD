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
        "Gerrit-Module: com.googlesource.gerrit.plugins.healthcheck.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.healthcheck.HttpModule",
        "Gerrit-ApiModule: com.googlesource.gerrit.plugins.healthcheck.HealthCheckExtensionApiModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "healthcheck_tests",
    srcs = glob(
        [
            "src/test/java/**/*Test.java",
        ],
        exclude = ["src/test/java/**/Abstract*.java"],
    ),
    resources = glob(["src/test/resources/**/*"]),
    deps = [
        ":healthcheck__plugin_test_deps",
    ],
)

[junit_tests(
    name = f[:f.index(".")].replace("/", "_"),
    srcs = [f],
    tags = ["owners"],
    visibility = ["//visibility:public"],
    deps = [
        ":healthcheck__plugin_test_deps",
    ],
) for f in glob(["src/test/java/**/*IT.java"])]

java_library(
    name = "healthcheck__plugin_test_deps",
    testonly = 1,
    srcs = glob(["src/test/java/**/Abstract*.java"]),
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":healthcheck__plugin",
    ],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":healthcheck__plugin",
    ],
)
