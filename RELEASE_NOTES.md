# Release notes

## 3.0.2 (2026)

Security hotfix. Scientific results are the same as in 3.0.1.

- Report mails ("report as faulty") show the text entered by the visitor as plain text; links or markup in the
  comment are no longer rendered. The contact address is checked and the comment is limited to 2,000 characters.
- The "search history" report can be sent again; if a mail cannot be sent, the dialog says so.
- Failed logins: the entered user name is no longer logged, and the fail2ban filter cannot be fooled by crafted
  user names. The fail2ban jail now reads the log of the service (it did not see any failed login before).
- The password reset link is built from the configured public address (`FI_PUBLIC_URL`), not from the request.
- User names and e-mail addresses are unique, independent of case (database migration V2).
- New installations: the initial admin needs `FI_ADMIN_PASSWORD` and `FI_ADMIN_MAIL`; no password is generated or
  logged. Generated passwords use a cryptographic random generator.
- Operation: `SERVER_ADDRESS` lets the application listen only on the address of the reverse proxy.

## 3.0.1 (2026)

### Scientific results

- The ion species **+H / −H** in the search and the molecular screening now use the **mass of the proton
  (1.007276 u)** instead of the mass of the hydrogen atom (1.0078 u). Measured masses therefore fit about 0.5 mDa
  better (about 2.8 ppm at m/z 189, 1 ppm at m/z 500).
- Compared with 2.x and 3.0.0, some substances are now found at narrow ppm windows (example: phenazone [M+H]⁺
  189.1022 with 1 ppm), and hits that were only just inside the window because of the old offset may drop out.
  In the regression data (5 ppm) 90 candidates were added and 24 dropped out, the best match changed for 30 of 242
  targets, and the median mass deviation went from −0.40 to +0.12 mDa.
- Unchanged: MetFrag (MS/MS), the REST API, searches with "±0".

## 3.0.0 (2026)

New user interface and a fully supported platform. Scientific results are the same as in 2.0.1.

### User interface (new)

- Modern web interface in the FOR-IDENT colours (Vaadin Flow) instead of the Vaadin 8 interface, German and English.
- Log in, registration, **password reset by e-mail link** (valid for one hour; before, a new random password was
  mailed), user settings; for administrators the user administration with **"Send test mail"**.
- Quick search, search and index search with filters per column, details with structure, web search and external
  links, Excel export, "report as faulty"; deep links `?stoffidentid=…` and `?inchikey=…` work as before.
- Molecular screening:
  - upload of up to 15 files and an RTI calibration file;
  - processes and score weights: the weights are free; if the active weights do not add up to 100 %, a pop-up
    offers to adjust them proportionally;
  - new parameters **"Use masses from formulae in the file"** and **"ppm for masses from formulae"** (defaults as
    before);
  - progress and summary, result list with filters and score bars, details with scoring and the **MS/MS spectrum
    with the MetFrag fragments**, the spectrum of each target, downloads (results, targets without results,
    complete with parameter sheets), help.
- External links: ECHA CHEM (search by CAS), EPA CompTox Dashboard, **PubChem** (by InChIKey) instead of
  Chemicalize (which now needs a login).

### File import

- Text format (SCIEX OS / MasterView report templates, Compound Discoverer scripts): read by keyword; the Formula
  line is optional, peaks may be in one or several lines (also with decimal comma), and a faulty entry no longer
  fails the whole file. Supported formats: see README.

### REST API

- Unchanged answers. New: description generated from the code, **Swagger UI at `/api/docs`**, OpenAPI document at
  `/api/openapi`.

### Operation

- **Java 21**, Spring Boot 4.1 (Spring 7, Spring Security 7, Tomcat 11, Jakarta EE 11), Vaadin 25, EclipseLink 5;
  no known vulnerabilities in the libraries (CVE scan).
- **Database migrations with Flyway**: existing databases are taken over unchanged as version 1.
- **Mail with SMTP login** (STARTTLS/SSL) and timeouts; health check `/actuator/health`; log to the systemd journal;
  MongoDB only when transformation products are switched on.
- Scripts for moving the databases to a new server and for daily backups; operations guide in `deploy/README.md`.
- Security: the session ID is only kept in a cookie (`SameSite=Lax`), never in URLs; failed logins are logged with
  the client address, so fail2ban can block password guessing (configuration in `deploy/fail2ban`).

### Fixes

- Empty files or files with more than 10,000 features are reported instead of being ignored silently.
- The RTI screening of the GC variant failed with an error (preparation of a GC instance).

### Known limitations

- The MassBank links in the details point to the old MassBank address and do not open any more (MassBank 3 uses new
  record IDs); planned for a later version.
- Tables exported from Compound Discoverer 3.x (Excel/CSV) are not recognised; use the text export.

## 2.0.1 (2026)

Fixes found in the first production test of 2.0.0:

- MetFrag (MS/MS scoring) failed with "Error loading InChI native code" when run from the
  packaged application: the processing thread did not see the libraries inside the JAR.
- Large files in the molecular screening gave no result or never finished: the searches
  started one thread per feature and hit the process limit of the server. The searches now use
  one thread per CPU; errors during processing now close the progress window and are reported.

## 2.0.0 (2026)

Modernised platform of FOR-IDENT. The user interface stays the same.

### Operation

- Runs on **Java 17** (before: Java 8); no `-javaagent` any more. See `deploy/README.md`.
- Database passwords and other settings are read from environment variables (`FI_*`), no
  credentials in the JAR or in the repository.
- Spring Boot 2.7.18, Spring Framework 5.3.39, Spring Security 5.8.16, Tomcat 9.0.123,
  Vaadin 8.14.3, EclipseLink 2.7.16, Apache POI 5.5.1, PostgreSQL JDBC 42.7.14 and further
  library updates; all known vulnerabilities of the libraries are fixed except those of the
  Spring 5.3 / Boot 2.7 line, which need the move to Spring 6 (planned).
- Build with Gradle 8 (wrapper), versions in `gradle/libs.versions.toml`, CVE scan
  (`gradlew dependencyCheckAggregate`), regression tests against golden files.

### Scientific results

- **MS/MS scoring with MetFrag 2.6.11** (before: MetFragLib 2.3 from 2016) and the chemistry
  library CDK 2.11 (before: 2.0).
- The precursor ion type is now passed to MetFrag as intended: **[M+H]⁺** in positive and
  **[M−H]⁻** in negative ionisation mode.
- MS/MS scores therefore differ slightly from version 1.x (median change of the normalised
  MS/MS score 0.009 on the scale 0–1 in the regression data); in a few cases the best match changes
  between isomers. Mass search, retention time index and the REST API give the same results as
  before.

### Fixes

- Excel export: values got lost at random (parallel row creation).
- Duplicate feature names in an input file: MS/MS scores and best matches were assigned to the
  wrong feature; duplicates now get the suffix `#2`, `#3`, …
- Uploaded Excel files were written back when they were closed.
- Uploaded files are no longer kept: they are deleted on logout or when the session ends after the
  browser window was closed (default timeout 5 minutes).
  Note for users: a session without an open browser window now ends after 5 minutes instead of
  30 minutes (an open window keeps the session alive); adjustable with `FI_SESSION_TIMEOUT`.
- The side bar shows the software version to administrators (was "null").
