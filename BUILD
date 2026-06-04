load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_test_util",
    "gerrit_plugin_tests",
)

PLUGIN = "healthcheck"

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

[gerrit_plugin_tests(
    name = f[:f.index(".")].replace("/", "_"),
    srcs = [f],
    plugin = PLUGIN,
    deps = [":healthcheck_test_util"],
) for f in glob(["src/test/java/**/*IT.java"])]

gerrit_plugin_test_util(
    name = "healthcheck_test_util",
    srcs = glob(["src/test/java/**/Abstract*.java"]),
    deps = [":healthcheck__plugin"],
)
