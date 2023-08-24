# Custom checks extensions

The @PLUGIN@ plugin allows other plugins to provide their own custom check that
should participate in the definition of healthiness of the node.

Plugins that are interested in doing so, will need to add a dependency on the
healthcheck plugin, for example, their `BUILD` file could be as such:

```
gerrit_plugin(
    name = "plugin-foo",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Implementation-Title: Foo plugin",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/foo",
        "Gerrit-PluginName: foo",
        "Gerrit-Module: com.googlesource.gerrit.plugins.foo.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "gerrit-healthcheck-neverlink",
    ],
)

java_library(
    name = "gerrit-healthcheck-neverlink",
    neverlink = True,
    exports = ["//plugins/healthcheck"],
)
```

Then, the plugin can provide its own healthcheck logic via a concrete
implementation of an `AbstractHealthCheck`, for example:

```java

@Singleton
public class FooHealthCheck extends AbstractHealthCheck {

    @Inject
    public FooHealthCheck(
            ListeningExecutorService executor,
            HealthCheckConfig config,
            @PluginName String name,
            MetricMaker metricMaker) {
        super(executor, config, name, metricMaker);
    }

    @Override
    protected Result doCheck() throws Exception {
        // health logic goes here
        return Result.PASSED;
    }
}
```

To build the plugin in the gerrit-CI, as
[documented](https://gerrit-review.googlesource.com/Documentation/dev-plugins.html#_cross_plugin_communication)
by gerrit, you should be configuring your build job as follows:

```yaml
- project:
    name: plugin-foo
    jobs:
      - 'plugin-{name}-bazel-{branch}':
          extra-plugins: 'healthcheck'
          branch:
            - master
```

To get the CI to validate your plugin you should also add the dependency to your `Jenkinsfile`:

```
pluginPipeline(
  formatCheckId: 'gerritforge:foo-format-3852e64366bb37d13b8baf8af9b15cfd38eb9227',
  buildCheckId: 'gerritforge:foo-3852e64366bb37d13b8baf8af9b15cfd38eb9227',
  extraPlugins: ['healthcheck'])
```

Hitting the healthcheck status endpoint will now report the `foo` check:

```shell
curl -v 'http://gerrit:8080/config/server/healthcheck~status'

{
  "elapsed": 18,
  "foo": {
    "result": "passed",
    "ts": 1683723806276,
    "elapsed": 0
  },
  "auth": {
    "result": "passed",
    "ts": 1683723806269,
    "elapsed": 4
  },
  "querychanges": {
    "result": "passed",
    "ts": 1683723806266,
    "elapsed": 3
  },
  "jgit": {
    "result": "passed",
    "ts": 1683723806258,
    "elapsed": 7
  },
  "projectslist": {
    "result": "passed",
    "ts": 1683723806265,
    "elapsed": 1
  },
  "ts": 1683723806258
}
```

and the metrics for such component will also be reported, out of the box.

```shell
foo_latest_latency{quantile="0.5",} 0.008638662
foo_latest_latency{quantile="0.75",} 0.008638662
foo_latest_latency{quantile="0.95",} 0.010553707
foo_latest_latency{quantile="0.98",} 0.147902061
foo_latest_latency{quantile="0.99",} 0.147902061
foo_latest_latency{quantile="0.999",} 0.147902061
foo_latest_latency_count 4.0
foo_failure_total 0.0
```
