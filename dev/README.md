# Local development environment (FOR-IDENT, LC)

Runs the application on Windows against a local PostgreSQL with the same database layout
as a production instance (profile `lc`). Transformation products are switched off, so no
MongoDB is needed.

## Prerequisites
* JDK 21 for the application and for Gradle (3.x; the Vaadin plugin needs Gradle on Java 21), and a successful `gradlew clean build`
* PostgreSQL 17 on `localhost:5432`, installed with the EDB installer, e.g.
  `postgresql-17.11-5-windows-x64.exe --mode unattended --unattendedmodeui none --superpassword <pw> --serverport 5432 --enable-components server,commandlinetools --disable-components pgAdmin,stackbuilder`
  * Recommended: `ALTER SYSTEM SET listen_addresses = 'localhost'` and restart the service
* **The JDBC driver of the application (PostgreSQL 42.1.4) does not support SCRAM-SHA-256**,
  the default authentication of PostgreSQL 14+. Until the driver is updated, the application
  role needs an md5 password and an md5 rule in `pg_hba.conf` before the default rules:
  ```
  host    all             forident        127.0.0.1/32            md5
  host    all             forident        ::1/128                 md5
  ```
  (`setup-local-db.ps1` creates the role; set `SET password_encryption = 'md5'` before
  `ALTER ROLE forident PASSWORD '...'`.)
* Compound database dumps (`pg_dump -Fc`), one file per database: `fi-lc-stoffident.dump`,
  `fi-lc-plantident.dump`.
  They are not part of the repository.

## Secrets
Copy `local.env.example` to `local.env` (ignored by Git) or keep the file outside the
repository and pass it with `-SecretsFile`. It holds the passwords of the PostgreSQL
superuser, of the application role `forident` and of the initial `admin` user.

## Setup and start
```
.\dev\setup-local-db.ps1 -SecretsFile <secrets file> -DumpDir <folder with the dumps>
.\dev\run-local.ps1 -SecretsFile <secrets file> -Init     # once: user DB schema, groups, admin
.\dev\run-local.ps1 -SecretsFile <secrets file>           # normal start
.\dev\run-local.ps1 -SecretsFile <secrets file> -TestAdmin # local UI tests, see below
```
`-TestAdmin` (local only, never on a server) adds the administrator `test-admin@example.test` with a new random
password on every start, written to `local-test-admin.env` next to the secrets file, and writes the password reset
links to the log (there is no mail server locally).
The application runs on http://localhost:8080. Log in with `admin` and
`FI_ADMIN_PASSWORD` (mail address `FI_ADMIN_MAIL` from the secrets file, default `admin@example.test`). The REST API is available without login, e.g.
http://localhost:8080/api/substances?inchiKey=VEQOALNAAJBPNY-UHFFFAOYSA-N

To run `-Init` again, drop and recreate the user database `fi-lc-users` first.

The home and help pages are read from `${user.home}/html` (`spring.resources.static-locations`)
and are not part of the repository, so the home page stays empty without them.

## Regression tests
`application/src/regressionTest` starts the complete application (profiles `lc`) against the
local databases and compares results with golden files in
`application/src/regressionTest/resources/golden`. The golden files are **not part of the published
repository**, because they contain extracts of the compound databases: create them once with `-Update` against
your own reference databases, then compare after every change.

| Test | What is compared |
|---|---|
| `ImportRegressionTest` | imported features and RTI calibration of all sample files (`resources/`, module test resources) |
| `ProcessingRegressionTest` | scored candidates of the complete processing (mass, RTI, MS/MS, MassBank) against the compound databases, UI default settings |
| `ExportRegressionTest` | cell texts of the Excel export of processing results |
| `RestApiRegressionTest` | status and body of the public REST API |

```
.\dev\run-regression.ps1 -SecretsFile <secrets file>                          # compare
.\dev\run-regression.ps1 -SecretsFile <secrets file> -Tests "*ImportRegressionTest"
.\dev\run-regression.ps1 -SecretsFile <secrets file> -ProductionUrl https://for-ident.example.org   # also compare the API with production
.\dev\run-regression.ps1 -SecretsFile <secrets file> -Update                  # rewrite the golden files
```
* Differing results are written to `application/build/regression/actual`; the test output names
  every differing file.
* Before comparing, the tests check the number of substances per database
  (`golden/database/substance-counts.txt`). The golden files are only valid for the reference
  data (the compound databases they were created with).
* Use `-Update` only deliberately (e.g. after accepted changes of the scientific results) and
  review the Git diff of the golden files.
* **Never put the output of `src/test` on the regression classpath**: its `TestApplicationContext`
  replaces the STOFF-IDENT data with test data and deletes all users.
