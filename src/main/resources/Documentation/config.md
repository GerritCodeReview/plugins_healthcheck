@PLUGIN@ configuration
======================

The @PLUGIN@
------------

The plugin does not require any configuration at all and exposes an HTTP
endpoint for querying the service status.

```
GET /config/server/healthcheck~status

)]}'
{
  "ts": 139402910202,
  "elapsed": 100,
  "querychanges": {
    "ts": 139402910202,
    "elapsed": 20,
    "result": "passed"
  },
  "projectslist": {
    "ts": 139402910202,
    "elapsed": 100,
    "result": "passed"
  },
  "auth": {
    "ts": 139402910202,
    "elapsed": 80,
    "result": "passed"
  }
}
```

Settings
--------

The plugin allows to customize its behaviour through a specific
`healthcheck.config` file in the `$GERRIT_SITE/etc` directory.

Each section of the form `[healthcheck "<checkName>"]` can tailor the
behaviour of an individual `<checkName>`. The section `[healthcheck]`
defines the global defaults valid for all checks.

The following check names are available:

- `querychanges`: check the ability to query changes
- `jgit` : check connectivity to the filesystem and ability to open a JGit ref and object
- `projectslist` : check the ability to list projects with their descriptions
- `auth`: check the ability to authenticate with username and password
- `activeworkers`: check the number of active worker threads and the ability to create a new one
- `deadlock` : check if Java deadlocks are reported by the JVM
- `blockedthreads` : check the number of blocked threads

Each check name can be disabled by setting the `enabled` parameter to **false**,
by default this parameter is set to **true**

The following parameters are available:

- `healthcheck.<checkName>.timeout` : Specific timeout (msec) for the
  healthcheck to complete. Zero means that there is no timeout.

  Default: 500

- `healthcheck.<checkName>.query` : Query to be executed for extracting
   elements from the check.

  Default: status:open

- `healthcheck.<checkName>.limit` : Maximum number of elements to retrieve from
  the the check results.

  Default: 10

- `healthcheck.jgit.project` : A project name to check for accessibility of its refs/meta/config.
   Multiple occurrences are allowed to configure more projects, in addition
   to the default ones that are always included.

  Default: All-Projects, All-Users

 - `healthcheck.auth.username` : Username to use for authentication

   Default: healthcheck

 - `healthcheck.auth.password` : Password to use for authentication
 
   Default: no password

 - `healthcheck.activeworkers.threshold` : Percent of queue occupancy above which queue is consider 
    as full.

   Default: 80

 - `healthcheck.blockedthreads.threshold` : Percent of all threads that are blocked above which instance
   is considered as unhealthy.

   Default: 50

By default `healthcheck.blockedthreads` is calculated as ratio of BLOCKED threads against the all
Gerrit threads. It might be not sufficient for instance in case of `SSH-Interactive-Worker` threads
that could be all blocked making effectively a Gerrit instance unhealthy (neither fetch nor push
would succeed) but the threshold could be still not reached. Therefore one can fine tune the check
by putting detailed configuration for one or more thread groups (all threads that have the name
starting with a given prefix) to be checked according to the following template:

```
[ healthcheck "blockedthreads" ]
    threshold = [prefix]=[XX]
```

Note that in case when specific thread groups are configured all threads are no longer checked.

* **Example 1:** _check if BLOCKED threads are above the limit of 70_

   ```
   [ healthcheck "blockedthreads" ]
       threshold = 70
   ```

* **Example 2:** _check if BLOCKED `foo` threads are above the 33 limit_

   ```
   [ healthcheck "blockedthreads" ]
       threshold = foo=33
   ```

* **Example 3:** _check if BLOCKED `foo` threads are above the 33 limit and if BLOCKED `bar`_
  _threads are above the the 60 limit_

   ```
   [ healthcheck "blockedthreads" ]
      threshold = foo=33
      threshold = bar=60
   ```
