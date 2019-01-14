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

## How to use

The healthcheck plugin exposes a single endpoint under its root URL and provides a JSON output of the
Gerrit health status.

The HTTP status code returned indicates whether Gerrit is healthy (HTTP status 200) or has some issues (HTTP status 500).

The HTTP response payload is a JSON output that contains the details of the checks performed.

- ts: epoch timestamp in millis of the test
- elapsed: elapsed time in millis to perform the check
- reviewdb: check that Gerrit can connect and query ReviewDb
- projectslist: check that Gerrit can list projects
- auth: check that Gerrit can authenticate users

Each check returns a JSON payload with the following information:

- ts: epoch timestamp in millis of the individual check
- elapsed: elapsed time in millis to complete the check
- passed: boolean indicating if the check passed

Example of a healthy Gerrit response:

```
GET /config/server/healthcheck~status

200 OK
Content-Type: application/json

{
  "ts": 139402910202,
  "elapsed": 100,
  "reviewdb": {
    "ts": 139402910202,
    "elapsed": 50,
    "passed": true
  },
  "projectslist": {
    "ts": 139402910202,
    "elapsed": 100,
    "passed": true
  },
  "auth": {
    "ts": 139402910202,
    "elapsed": 80,
    "passed": true
  }
}```

Example of a Gerrit instance with the projects list not available:

```
GET /config/server/healthcheck~status

500 ERROR
Content-Type: application/json

{
  "ts": 139402910202,
  "elapsed": 100,
  "reviewdb": {
    "ts": 139402910202,
    "elapsed": 50,
    "passed": true
  },
  "projectslist": {
    "ts": 139402910202,
    "elapsed": 100,
    "passed": false
  },
  "auth": {
    "ts": 139402910202,
    "elapsed": 80,
    "passed": true
  }
}
```

