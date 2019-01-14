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

- reviewdb: boolean (true/false) status of ReviewDb
- projectslist: boolean (true/false) Gerrit can list projects
- auth: boolean (true/false) Gerrit authentication

Example of a healthy Gerrit response:

```
GET /config/server/healthcheck~status

200 OK
Content-Type: application/json

{ "reviewdb": true, "projectslist": true, "auth": true }
```

Example of a Gerrit instance with the projects list not available:

```
GET /config/server/healthcheck~status

500 ERROR
Content-Type: application/json

{ "reviewdb": true, "projectslist": false, "auth": true }
```

