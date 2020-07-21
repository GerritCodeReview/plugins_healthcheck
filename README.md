# Plugin to verify the Gerrit health status

Allow having a single entry point to check the availability
of the services that Gerrit exposes.

## How to build

Clone or link this plugin to the plugins directory of Gerrit's source tree, and then run bazel build on the plugin's directory.

Example:

```
git clone --recursive https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/plugins/healthcheck
pushd gerrit/plugins && ln -s ../../healthcheck . && popd
cd gerrit && bazel build plugins/healthcheck
```

The output plugin jar is created in:

```
bazel-genfiles/plugins/healthcheck/healthcheck.jar
```

## How to install

Copy the healthcheck.jar into the Gerrit's /plugins directory and wait for the plugin to be automatically loaded.
The healthcheck plugin is compatible with both primary Gerrit setups and Gerrit replicas. The only difference to bear
in mind is that some checks may not be successful on replicas (e.g. query changes) because the associated subsystem is
switched off.

## How to use

The healthcheck plugin exposes a single endpoint under its root URL and provides a JSON output of the
Gerrit health status.

The HTTP status code returned indicates whether Gerrit is healthy (HTTP status 200) or has some issues (HTTP status 500).

The HTTP response payload is a JSON output that contains the details of the checks performed.

- ts: epoch timestamp in millis of the test
- elapsed: elapsed time in millis to perform the check
- querychanges: check that Gerrit can query changes
- reviewdb: check that Gerrit can connect and query ReviewDb
- projectslist: check that Gerrit can list projects
- jgit: check that Gerrit can access repositories

Each check returns a JSON payload with the following information:

- ts: epoch timestamp in millis of the individual check
- elapsed: elapsed time in millis to complete the check
- result: result of the health check

  - passed: the check passed successfully
  - disabled: the check was disabled
  - failed: the check failed with an error
  - timeout: the check took too long and timed out

Example of a healthy Gerrit response:

```
GET /config/server/healthcheck~status

200 OK
Content-Type: application/json

)]}'
{
  "ts": 139402910202,
  "elapsed": 100,
  "querychanges": {
    "ts": 139402910202,
    "elapsed": 20,
    "result": "passed"
  },
  "reviewdb": {
    "ts": 139402910202,
    "elapsed": 50,
    "result": "passed"
  },
  "projectslist": {
    "ts": 139402910202,
    "elapsed": 100,
    "result": "passed"
  },
  "jgit": {
    "ts": 139402910202,
    "elapsed": 80,
    "result": "passed"
  }
}
```

Example of a Gerrit instance with the projects list timing out:

```
GET /config/server/healthcheck~status

500 ERROR
Content-Type: application/json

)]}'
{
  "ts": 139402910202,
  "elapsed": 100,
  "querychanges": {
    "ts": 139402910202,
    "elapsed": 20,
    "result": "passed"
  },
  "reviewdb": {
    "ts": 139402910202,
    "elapsed": 50,
    "result": "passed"
  },
  "projectslist": {
    "ts": 139402910202,
    "elapsed": 100,
    "result": "timeout"
  },
  "jgit": {
    "ts": 139402910202,
    "elapsed": 80,
    "result": "passed"
  }
}
```

## Metrics

As for all other endpoints in Gerrit, some metrics are automatically emitted when the  `/config/server/healthcheck~status`
endpoint is hit (thanks to the [Dropwizard](https://metrics.dropwizard.io/3.1.0/manual/core/) library).

Some additional metrics are also produced to give extra insights on their result about results and latency of healthcheck
sub component, such as jgit, reviewdb, etc.

More information can be found in the [config.md](resources/Documentation/config.md) file.
