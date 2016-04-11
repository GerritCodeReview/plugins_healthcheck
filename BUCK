include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//lib/maven.defs')

DEPS = [
]
PROVIDED_DEPS = [
]
TEST_DEPS = GERRIT_PLUGIN_API + [
  ':healthcheck__plugin',
  '//lib:junit',
  '//lib:truth',
]

gerrit_plugin(
  name = 'healthcheck',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: healthcheck',
    'Gerrit-Module: com.googlesource.gerrit.plugins.healthcheck.Module',
    'Gerrit-SshModule: com.googlesource.gerrit.plugins.healthcheck.SshModule',
  ],
  deps = DEPS,
  provided_deps = PROVIDED_DEPS,
)

java_library(
  name = 'classpath',
  deps = [':healthcheck__plugin'],
)

java_test(
  name = 'healthcheck_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['healthcheck'],
  source_under_test = [':healthcheck__plugin'],
  deps = TEST_DEPS,
)

