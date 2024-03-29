## Metrics

As for all other endpoints in Gerrit, some metrics are automatically emitted when the  `/config/server/healthcheck~status`
endpoint is hit (thanks to the [Dropwizard](https://metrics.dropwizard.io/3.1.0/manual/core/) library).
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
* plugins_healthcheck_<healthcheck_component>_latest_measured_latency: the latency of this component in the latest executed healthcheck run
* plugins_healthcheck_jgit_failure_total: the cumulative number of failures for this specific component

```
# HELP plugins_healthcheck_jgit_latest_measured_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/jgit/latency, type=com.google.gerrit.metrics.dropwizard.CallbackMetricImpl0$1)
# TYPE plugins_healthcheck_jgit_latest_measured_latency gauge
plugins_healthcheck_jgit_latest_measured_latency 4.0

# HELP plugins_healthcheck_projectslist_latest_measured_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/projectslist/latency, type=com.google.gerrit.metrics.dropwizard.CallbackMetricImpl0$1)
# TYPE plugins_healthcheck_projectslist_latest_measured_latency gauge
plugins_healthcheck_projectslist_latest_measured_latency 5.0

# HELP plugins_healthcheck_blockedthreads_latest_measured_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/blockedthreads/latency, type=com.google.gerrit.metrics.dropwizard.CallbackMetricImpl0$1)
# TYPE plugins_healthcheck_blockedthreads_latest_measured_latency gauge
plugins_healthcheck_blockedthreads_latest_measured_latency 6.0

# HELP plugins_healthcheck_changesindex_latest_measured_latency Generated from Dropwizard metric import (metric=plugins/healthcheck/changesindex/latency, type=com.google.gerrit.metrics.dropwizard.CallbackMetricImpl0$1)
# TYPE plugins_healthcheck_changesindex_latest_measured_latency gauge
plugins_healthcheck_changesindex_latest_measured_latency 3.0

# HELP plugins_healthcheck_jgit_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/jgit/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_jgit_failure_total counter
plugins_healthcheck_jgit_failure_total 3.0

# HELP plugins_healthcheck_projectslist_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/projectslist/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_projectslist_failure_total counter
plugins_healthcheck_projectslist_failure_total 0.0

# HELP plugins_healthcheck_blockedthreads_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/blockedthreads/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_blockedthreads_failure_total counter
plugins_healthcheck_blockedthreads_failure_total 1.0

# HELP plugins_healthcheck_changesindex_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/changesindex/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_changesindex_failure_total counter
plugins_healthcheck_changesindex_failure_total 1.0
```

Note that additionally to the default `blockedthreads` metrics pair failures counter will reported for
each configured prefix. For given config:

```
[healthcheck "blockedthreads"]
    threshold = Foo=33
```

the following additional metric will be exposed and populated:

```
# HELP plugins_healthcheck_blockedthreads_foo_failure_total Generated from Dropwizard metric import (metric=plugins/healthcheck/blockedthreads-foo/failure, type=com.codahale.metrics.Meter)
# TYPE plugins_healthcheck_blockedthreads_foo_failure_total counter
plugins_healthcheck_blockedthreads_foo_failure_total 2.0
```

Note that prefix is used as postfix for a metric name but it is lower-cased and sanitized as only
`a-zA-Z0-9_-/` chars are allowed to be a metric name (chars outside this set are turned to `_`).

Metrics will be exposed to prometheus by the [metrics-reporter-prometheus](https://gerrit.googlesource.com/plugins/metrics-reporter-prometheus/) plugin.
