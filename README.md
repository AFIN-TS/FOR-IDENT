# FOR-IDENT 2.0

FOR-IDENT 2.0 is a modernised version of the FOR-IDENT platform (Java 21, Spring Boot 4, Vaadin Flow 25, PostgreSQL
17), based on the original source code of the University of Applied Sciences Weihenstephan-Triesdorf
([go4st/FOR-IDENT](https://github.com/go4st/FOR-IDENT), branch `master`, 2019-12-16). The modernisation was carried
out by **AFIN-TS GmbH** in 2026; what was changed is listed in [NOTICE.md](NOTICE.md) and, per version, in
[RELEASE_NOTES.md](RELEASE_NOTES.md). Current software version: 3.0.1.

No compound databases are included: STOFF-IDENT, PLANT-IDENT and the other databases have to be provided by the
operator of an instance.

## About FOR-IDENT
The FOR-IDENT project is a research project sponsored by the german Federal Ministry of Education and Research
with the purpose to improve the identification of organic trace substances by merging resources and standardization
of suspected- and non-target analysis. The original GitHub project published the result of software development implemented
by the University of Applied Sciences Weihenstephan-Triesdorf. This software is a Java web application based on
the Spring Framework and Vaadin. 

https://www.for-ident.org (Official project website)

https://water.for-ident.org (Official production instance)

## Getting Started
To get a copy of the project clone this GitHub project. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
* **JDK 21** (e.g. Eclipse Temurin 21) for compiling, tests and running the application (Java toolchain). If none
  is installed, Gradle downloads it into the Gradle user home
* **JDK 21** also to run Gradle itself (`JAVA_HOME`): the Vaadin Gradle plugin requires it
* The frontend bundle of the user interface is built with Node.js, which the Vaadin plugin downloads into the user
  home (`~/.vaadin`) if it is not installed
* Version 3.x: the user interface is the Vaadin Flow module `ui.flow` (Vaadin 25). The Vaadin 8 UI (`ui.vaadin`,
  `i18n.vaadin`) of 1.x/2.x has been removed. The REST API works as before
  (changes per version: `RELEASE_NOTES.md`)
* No Java agent needed: the JPA entities are woven at build time (`gradle/eclipselink-weaving.gradle`)
* No local Gradle installation needed, the Gradle wrapper (8.14) is used
* **Database (PostgreSQL recommended)**  

### Installing
Build the project with the wrapper, `JAVA_HOME` pointing to a JDK 21
```
gradlew clean build
```
Further build tasks:

| Task | Result |
|---|---|
| `gradlew :application:asciidoctor` | API overview `application/build/docs/asciidoc/api.html`. The complete API description is generated from the code: Swagger UI at `/api/docs`, OpenAPI document at `/api/openapi` of the running application |
| `gradlew :documentation:asciidoctor` | Project documentation `documentation/build/docs/index.html` |
| `gradlew dependencyCheckAggregate` | Known vulnerabilities (CVE) of all libraries: `build/reports/dependency-check-report.html`. Needs an NVD API key in `NVD_API_KEY` (free; without it the download of the NVD data fails). `dev/run-cve-scan.ps1` reads it from the local secrets file |

Versions of all libraries and plugins are maintained in `gradle/libs.versions.toml`.

The software properties for each profile are found in **application/src/main/resources/config**.

At least 1 user management and 1 compound database connection are needed (spring database properties
`spring.datasource.user` and `spring.datasource.*yourDatabaseName*`).
Example compound database implementations can be found in modules :
* **search.service.search.stoffident**
* **search.service.search.plantident**

The compound data must be provided!

### Configuration
Credentials and hosts are **not** stored in the property files. They are read from environment variables
(Spring placeholders `${VARIABLE:default}`), so no secrets end up in the repository.

| Variable | Used for |
|---|---|
| `FI_DB_USER_URL`, `FI_DB_USER_USERNAME`, `FI_DB_USER_PASSWORD` | User management database (all profiles) |
| `FI_DB_STOFFIDENT_URL`, `FI_DB_STOFFIDENT_USERNAME`, `FI_DB_STOFFIDENT_PASSWORD` | STOFF-IDENT compound database (LC profiles) |
| `FI_DB_PLANTIDENT_URL`, `FI_DB_PLANTIDENT_USERNAME`, `FI_DB_PLANTIDENT_PASSWORD` | PLANT-IDENT compound database (LC profiles) |
| `FI_DB_DUFTSTOFFIDENT_URL`, `FI_DB_DUFTSTOFFIDENT_USERNAME`, `FI_DB_DUFTSTOFFIDENT_PASSWORD` | DuftSTOFF-IDENT compound database (GC profiles `gc`, `development-gc`, `initialization-gc`) |
| `FI_DB_PATHWAY_HOST`, `FI_DB_PATHWAY_PORT`, `FI_DB_PATHWAY_DATABASE` | MongoDB with transformation pathways (defaults: `localhost`, `27017`, `pathwaydb`) |
| `FI_MAIL_HOST`, `FI_MAIL_PORT` | SMTP server (default port `25`; the GC production profile defaults to `localhost`) |
| `FI_MAIL_USERNAME`, `FI_MAIL_PASSWORD`, `FI_MAIL_SMTP_AUTH`, `FI_MAIL_STARTTLS`, `FI_MAIL_SSL` | SMTP login of a mailbox and encryption (see `deploy/README.md`, section 8) |
| `FI_CONTACT_ADDRESSES`, `FI_FROM_EMAIL_ADDRESS` | Contact and sender e-mail addresses |
| `FI_ADMIN_PASSWORD`, `FI_ADMIN_MAIL` | Password and mail address of the initial admin (initialization profiles only, required there, see below) |
| `FI_PUBLIC_URL` | Public address of the site, e.g. `https://for-ident.example.org`; links in mails (password reset) are built from it. Set it on every server |
| `SERVER_ADDRESS` | Address the application listens on (Spring Boot); on a server only the address the reverse proxy reaches it on |
| `FI_PATHWAY_ENABLED` | Transformation products (needs the MongoDB pathway database), default `false` |
| `FI_SESSION_TEMP_DIRECTORY` | Root of the temporary session directories with the uploaded files, default `<java.io.tmpdir>/fi-sessions-<port>`; emptied on startup |
| `FI_SESSION_TIMEOUT` | Session timeout, default `5m`; uploaded files are deleted when the session ends |

### Uploaded files
Uploaded files are not stored permanently. Each session has its own directory (readable only by the
user running the application), which is deleted when the session ends:
* **Logout:** immediately.
* **Browser window closed:** the Vaadin heartbeat (every 60 s) keeps the session of an open window
  alive; after the window was closed the session times out (`FI_SESSION_TIMEOUT`, 5 minutes) and the
  directory is deleted.
* **Server restart or crash:** all session directories are deleted on startup.

### Supported import formats
Files for the molecular screening (status SP-15, 2026-11). The format is recognised from the file content, not
from the name; at most 15 files of 15 MB per upload, 10,000 features per file.

| Format | File | Recognised by | Test |
|---|---|---|---|
| Text, MSP-like (`NAME:` / `RETENTIONTIME:` / `PRECURSORMZ:` / optional `Formula:` / peaks, blocks ended by `//`): SCIEX OS and MasterView report templates, Compound Discoverer scripts | `.txt` | keywords (any order; peaks in one or several lines, blank/tab separated, decimal comma accepted) | `TxtImportSciexTest`, `TxtImportVariantsTest`, regression (7 files) |
| Agilent MassHunter (CEF) | `.cef` | XML structure | `XMLImportCefTest`, regression (3 files) |
| Agilent MassHunter (Excel template) | `.xls(x)` | columns of `rti_agilent_config.xml` | `ExcelImportAgilentTest`, regression |
| Agilent ProFinder (Excel template) | `.xls(x)` | columns of `rti_agilent_profinder_config.xml` | `ExcelImportAgilentProfinderTest`, regression |
| AB SCIEX (Excel template) | `.xls(x)` | `rti_absciex_config.xml` | `ExcelImportABSciexTest`, regression |
| SCIEX MasterView (Excel, also with MS/MS sheets) | `.xls(x)` | `rti_sciex_masterview_config.xml`, `rti_sciex_msms_config.xml` | `ExcelImportSciexMasterView*Test`, regression |
| Thermo Compound Discoverer 2.x (Excel / CSV table) | `.xls(x)`, `.csv` | columns `Name`, `Molecular Weight`, `RT [min]` (CSV: fixed header line) | `ExcelImportThermoCompoundDiscovererTest`, `CsvImportTest`, regression |
| Thermo TraceFinder 3.2 (Excel / CSV) | `.xls(x)`, `.csv` | `rti_thermo_tracefinder_3_2_config.xml` / fixed header line | `ExcelImportGeneratedFormatsTest`, `CsvImportTest` |
| Thermo SIEVE 2.1 (Excel) | `.xls(x)` | `rti_thermo_cieve_2_1_config.xml` | `ExcelImportThermoCiecveTest`, regression |
| Waters UNIFI, MarkerLynx (marker / result table) | `.xls(x)` | `rti_waters_*_config.xml` | `ExcelImportWaters*Test`, regression |
| Bruker (CSV) | `.csv` | fixed header line | `CsvImportTest` |
| Generic template (sheet `targets`) | `.xls(x)` | `rti_tu_letzel_config.xml` | `ExcelImportGeneratedFormatsTest` |
| JSON | `.json` | structure | `JsonImportTest`, regression |
| mzML | `.mzML` | XML structure | regression (5 files) |
| RTI calibration (separate file or sheet `calibration`) | `.xls(x)` | `rti_calibration_config_logd.xml` (`substance name`, `rt…`, `logD`; data from row 4) | `ExcelImportCalibrationTest`, regression |

Compound Discoverer 3.x tables (`Calc. MW` instead of `Molecular Weight`) are not recognised; use the text export.
The Excel and text formats can be checked with the templates offered on the help page.

Example (PowerShell):
```
$env:FI_DB_USER_URL = "jdbc:postgresql://localhost:5432/fi-user"
$env:FI_DB_USER_USERNAME = "fi"
$env:FI_DB_USER_PASSWORD = "<password>"
```

Spring profile **initialization** (`initialization-lc` / `initialization-gc`) creates the user **admin** on startup
for the initial log in. Its password is taken from `FI_ADMIN_PASSWORD` and its mail address from `FI_ADMIN_MAIL`;
both are required, the application does not start in this profile without them (since 3.0.2). Change the password
after the first login.
Run the application (development mode)
```
gradle bootRun -x test
```

## Deployment
Build JAR
```
gradle clean build
```
The executable application jar will be build in **application/build/libs/de.hswt.fi.application.jar**

Run JAR
```
java -jar *PATH_TO_JAR* --spring.profiles.active=*DESIRED_SPRING_PROFILE* --server.port=*PORT*
```

Server installation as systemd service (Java 21, environment file for the credentials, rollback): see
**deploy/README.md**. After a deployment, `dev/smoke-test.sh https://<host>` checks UI, static pages and
REST API. Changes per version: **RELEASE_NOTES.md**.

## Running the tests
Run tests
```
gradle test
```
The test consists of unit tests for the importers, services, etc. and some integration and api tests.

## Built With
* [Spring Boot](https://spring.io/projects/spring-boot) - Java framework
* [Vaadin](https://www.vaadin.com) - The UI framework
* [Gradle](https://gradle.org) - Build Tool

## Authors
Original FOR-IDENT platform (University of Applied Sciences Weihenstephan-Triesdorf):
* **Marco Luthardt**
* **Tobias Placht**
* **August Gilg**

FOR-IDENT 2.0 (modernisation 2026): **AFIN-TS GmbH**, https://www.afin-ts.de. Changed and new files are
marked in [NOTICE.md](NOTICE.md) and in the Git history.

## License
This project is licensed under the GNU Lesser General Public License **LGPL v2.1** - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

### Special thanks go to 

Our sponsor: 
* **Federal Ministry of Education and Research**

Our partners :
* **Technical University of Munich**
* **Bavarian State Ministry of the Environment**
* **Zweckverband Landeswasserversorgung (state water supply)**
* **Berliner Wasserbetriebe (water supply)**

Collaborators during the project :
* **Environmental Protection Agency**
* **Metfrag**
* **Chemaxon**
* **Agilent, Bruker, Sciex, Thermo Fischer, Waters**
