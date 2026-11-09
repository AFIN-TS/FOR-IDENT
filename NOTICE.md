# NOTICE – FOR-IDENT 2.0

## Origin

FOR-IDENT 2.0 is a modified version of the **FOR-IDENT platform**:

- Authors: Marco Luthardt, Tobias Placht, August Gilg (University of Applied Sciences Weihenstephan-Triesdorf, HSWT)
- Funding: German Federal Ministry of Education and Research (BMBF)
- Source: https://github.com/go4st/FOR-IDENT, branch `master` (last commit 2019-12-16)
- Licence: GNU Lesser General Public License v2.1 ([LICENSE.md](LICENSE.md))

Some extensions come from the later development by August Gilg in branch `henry` of the same repository
(2021–2022), for example:
- the combined RTI/Henry index results with logD and signal values;
- the switch for transformation products;
- fixes in the Excel import and export.

## Modifications (2026, AFIN-TS GmbH)

The modified version is distributed under the same licence, LGPL v2.1. All changes were made in 2026 by AFIN-TS GmbH
(https://www.afin-ts.de). The changes of each version are listed in [RELEASE_NOTES.md](RELEASE_NOTES.md), and the
Git history shows each changed file in detail. Main changes:

- Platform: Java 21, Spring Boot 4.1, Jakarta EE, Gradle 8.14, current libraries (Apache POI, PostgreSQL JDBC, CDK,
  MetFrag 2.6.11, jmzML and others).
- User interface: the Vaadin 8 interface (`ui.vaadin`, `i18n.vaadin`) was replaced by a new Vaadin Flow 25 interface
  (`ui.flow`).
- Security: no stored passwords (credentials via environment variables), CSRF protection, password reset by e-mail
  link, session handling, logging of failed logins.
- Databases: schema migrations with Flyway; PostgreSQL 17.
- REST API: OpenAPI description and Swagger UI; updated external links.
- Vendor import formats checked and extended; regression test harness (golden files not included).
- Operations: systemd service, backups, reverse proxy and fail2ban examples (`deploy/`); local development scripts (`dev/`).
- Scientific change in 3.0.1: the ion species +H/−H use the proton mass (see RELEASE_NOTES.md).

## Third-party components in this repository

| Component | Location | Licence |
|---|---|---|
| MetFragLib 2.6.11 (classes only, built from the MetFragRelaunched release with `dev/metfraglib`) | `libraries/metfrag/` | LGPL v2.1 |
| Gradle wrapper | `gradle/wrapper/` | Apache License 2.0 |
| Sample and test files (vendor exports, mzML samples, Excel templates) | `resources/`, test resources | from the original FOR-IDENT repository |

All other libraries are downloaded by Gradle from Maven repositories under their own licences.

## Names and logos

"FOR-IDENT" and the FOR-IDENT logo belong to the original project. If you run a modified instance, name it as a
modified version.
