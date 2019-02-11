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
- `reviewdb` : check connectivity and ability to query ReviewDb
- `jgit` : check connectivity to the filesystem and ability to open a JGit ref and object
- `projectslist` : check the ability to list projects with their descriptions

The follwing parameters are available:

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