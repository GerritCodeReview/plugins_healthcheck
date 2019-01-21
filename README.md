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
- jgit: check that Gerrit can access repositories 

Each check returns a JSON payload with the following information:

- ts: epoch timestamp in millis of the individual check
- elapsed: elapsed time in millis to complete the check
- result: result of the health check
  
  - passed: the check passed successfully
  - failed: the check failed with an error
  - timeout: the check took too long and timed out

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

{
  "ts": 139402910202,
  "elapsed": 100,
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
endpoint it hit (thanks to the [Dropwizard](https://metrics.dropwizard.io/3.1.0/manual/core/) library).
For example, some of the metrics exposed will be:

```
# HELP http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint Generated from Dropwizard metric import (metric=http/server/rest_api/response_bytes/healthcheck-com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint, type=com.codahale.metrics.Histogram)
# TYPE http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint summary
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.5",} 308.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.75",} 308.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.95",} 308.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.98",} 310.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.99",} 310.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.999",} 310.0
http_server_rest_api_response_bytes_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_count 4.0

# HELP http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint Generated from Dropwizard metric import (metric=http/server/rest_api/server_latency/healthcheck-com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint, type=com.codahale.metrics.Timer)
# TYPE http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint summary
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.5",} 0.008638662
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.75",} 0.008638662
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.95",} 0.010553707
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.98",} 0.147902061
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.99",} 0.147902061
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint{quantile="0.999",} 0.147902061
http_server_rest_api_server_latency_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_count 4.0

# HELP http_server_rest_api_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_total Generated from Dropwizard metric import (metric=http/server/rest_api/count/healthcheck-com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint, type=com.codahale.metrics.Meter)
# TYPE http_server_rest_api_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_total counter
http_server_rest_api_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_total 4.0

# HELP http_server_rest_api_error_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_500_total Generated from Dropwizard metric import (metric=http/server/rest_api/error_count/healthcheck-com.googlesource.gerrit.plugins.healthcheck.api.HealthCheckStatusEndpoint/500, type=com.codahale.metrics.Meter)
# TYPE http_server_rest_api_error_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_500_total counter
http_server_rest_api_error_count_healthcheck_com_googlesource_gerrit_plugins_healthcheck_api_HealthCheckStatusEndpoint_500_total 2.0
```

However some extra metrics are also emitted to expose more details about the healthcheck result.
Specifically two metrics are emitted for each component contributing to the overall healthcheck result (JGIT, PROJECTLIST, REVIEWDB).
* plugins_healthcheck_<healthcheck_component>_latency: the cumulative latency (in ms) of performing the healthcheck for this specific component
* plugins_healthcheck_jgit_failure_total: the cumulative number of failures for this specific component

```
# HELP plugins_healthcheck_jgit_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/jgit/latency, type=com.codahale.metrics.Timer)
# TYPE plugins_healthcheck_jgit_latency summary
plugins_healthcheck_jgit_latency{quantile="0.5",} 0.002
plugins_healthcheck_jgit_latency{quantile="0.75",} 0.002
plugins_healthcheck_jgit_latency{quantile="0.95",} 0.002
plugins_healthcheck_jgit_latency{quantile="0.98",} 0.002
plugins_healthcheck_jgit_latency{quantile="0.99",} 0.002
plugins_healthcheck_jgit_latency{quantile="0.999",} 0.002
plugins_healthcheck_jgit_latency_count 1.0

# HELP plugins_healthcheck_projectslist_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/projectslist/latency, type=com.codahale.metrics.Timer)
# TYPE plugins_healthcheck_projectslist_latency summary
plugins_healthcheck_projectslist_latency{quantile="0.5",} 0.001
plugins_healthcheck_projectslist_latency{quantile="0.75",} 0.001
plugins_healthcheck_projectslist_latency{quantile="0.95",} 0.001
plugins_healthcheck_projectslist_latency{quantile="0.98",} 0.001
plugins_healthcheck_projectslist_latency{quantile="0.99",} 0.001
plugins_healthcheck_projectslist_latency{quantile="0.999",} 0.001
plugins_healthcheck_projectslist_latency_count 1.0

# HELP plugins_healthcheck_reviewdb_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/reviewdb/latency, type=com.codahale.metrics.Timer)
# TYPE plugins_healthcheck_reviewdb_latency summary
plugins_healthcheck_reviewdb_latency{quantile="0.5",} 0.001
plugins_healthcheck_reviewdb_latency{quantile="0.75",} 0.001
plugins_healthcheck_reviewdb_latency{quantile="0.95",} 0.001
plugins_healthcheck_reviewdb_latency{quantile="0.98",} 0.001
plugins_healthcheck_reviewdb_latency{quantile="0.99",} 0.001
plugins_healthcheck_reviewdb_latency{quantile="0.999",} 0.001
plugins_healthcheck_reviewdb_latency_count 1.0

# HELP plugins_healthcheck_jgit_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/jgit/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_jgit_failure_total counter
plugins_healthcheck_jgit_failure_total 0.0

# HELP plugins_healthcheck_projectslist_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/projectslist/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_projectslist_failure_total counter
plugins_healthcheck_projectslist_failure_total 0.0

# HELP plugins_healthcheck_reviewdb_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/reviewdb/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_reviewdb_failure_total counter
plugins_healthcheck_reviewdb_failure_total 0.0
```

Metrics will be exposed to prometheus by the [metrics-reporter-prometheus](https://gerrit.googlesource.com/plugins/metrics-reporter-prometheus/) plugin.
