# Operations guide: FOR-IDENT 3.x

Installation on a new Linux server, data migration, mail, backups, updates and checks (SP-16). Server-specific
details (host names, accounts, passwords) do not belong in this repository; `<host>` stands for the public name.

**Architecture:** the application runs as systemd service `for-ident` (Java 21, user `forident`, port 8080, no own
TLS) next to PostgreSQL 17 on the same server. A reverse proxy (Caddy) provides HTTPS with Let's Encrypt. Logs go to
the systemd journal. MongoDB is not needed (only for the transformation products, `FI_PATHWAY_ENABLED=false`).

## 1. Requirements

| | |
|---|---|
| Operating system | Ubuntu 24.04 LTS or newer (KVM server; systemd, cgroup v2) |
| Java | OpenJDK 21 runtime (`openjdk-21-jre-headless`) or Eclipse Temurin 21 |
| Memory | 4 GB heap (`-Xmx4G`) plus about 1 GB for the JVM; 8 GB RAM for the server with other services |
| Database | PostgreSQL 17 (package of the distribution, or from apt.postgresql.org) on localhost |
| Reverse proxy | Caddy 2 (on the host or `caddy-docker-proxy` in Docker), ports 80/443 open |
| Mail | SMTP access of an existing mailbox of the domain (topic T5) |

## 2. Files

| From the repository / build | Target on the server |
|---|---|
| `application/build/libs/de.hswt.fi.application.jar` | `/opt/for-ident/de.hswt.fi.application.jar` (root:forident, 644) |
| Static pages (`home_de.html`, `home_en.html`, `processing_help_*.html`, `impressum.html`, `css/`, `logos/`, `download/`) | `/opt/for-ident/html/` |
| `deploy/for-ident.service` | `/etc/systemd/system/for-ident.service` |
| `deploy/for-ident.env.example`, filled in | `/etc/for-ident/for-ident.env` (root:forident, **640**) |
| `deploy/db/*.sh` | `/opt/for-ident/bin/` (root, 755) |
| `deploy/for-ident-backup.service`, `.timer` | `/etc/systemd/system/` |
| `deploy/caddy/Caddyfile.example` or `docker-compose.override.example.yml` | Caddy configuration |

## 3. Build

```
gradlew clean build                 (with BUILD_NUMBER=<n> for the version 3.0.<n>)
```

Always build with `clean` for a release: otherwise old frontend bundles can remain in the JAR.

## 4. Prepare the server

```
sudo apt install openjdk-21-jre-headless postgresql
sudo useradd --system --create-home --shell /usr/sbin/nologin forident
sudo mkdir -p /opt/for-ident/html /opt/for-ident/bin /etc/for-ident
```

- Journal retention before the services start (personal data in logs, e.g. IP addresses of the proxy):
  `/etc/systemd/journald.conf.d/retention.conf` with `[Journal]`, `SystemMaxUse=500M`, `MaxRetentionSec=7day`.
- Firewall (`ufw`): incoming only 22, 80, 443 (and what other services need). Port 8080 only from the reverse
  proxy: Caddy on the host needs no rule; Caddy in Docker needs `ufw allow from <docker network> to any port 8080 proto tcp`
  (network of `docker network inspect caddy`). Without this rule Caddy answers 502.

## 5. Databases

PostgreSQL listens on localhost only (default). The application uses the role `forident` with a password
(SCRAM-SHA-256, the default since PostgreSQL 14; the JDBC driver of 3.x supports it).

### 5a. Move the existing databases (from the old server)

On the old server (PostgreSQL 12; the custom-format dumps are restored by the newer `pg_restore`):

```
sudo -u postgres bash export-databases.sh /var/tmp/fi-db-export
scp -r <old>:/var/tmp/fi-db-export /var/tmp/          (on the new server; directory mode 700)
```

On the new server:

```
sudo chown -R postgres: /var/tmp/fi-db-export
sudo -u postgres bash /opt/for-ident/bin/import-databases.sh /var/tmp/fi-db-export
sudo -u postgres psql -c '\password forident'           (if the role was created by the script)
sudo rm -r /var/tmp/fi-db-export                     (also on the old server: the user database contains personal data)
```

The import checks the checksums, creates the databases owned by `forident`, restores them and compares the row count
of every table with the export. It stops if a database already exists or a count differs.

### 5b. New installation without existing data

Create the three databases owned by `forident`, then start the application once with the additional profile
`initialization-lc` and `FI_ADMIN_PASSWORD` set: the schema migrations create the tables, the profile creates the
groups and the user `admin`. The substance data come from the database dumps of the data provider.

### Schema migrations (Flyway)

The application migrates its databases on startup (`application/src/main/resources/db/migration`): version 1 is
the schema of FOR-IDENT 2.x. Existing databases are recorded as version 1 without changes (table
`flyway_schema_history`), empty databases get their tables. Schema changes of later versions come as `V2__…sql`,
`V3__…sql` and run automatically once; existing scripts are never changed. **Before an update with a new migration
make a backup** (section 9): a migration cannot be undone by the old JAR.

## 6. Application

1. JAR, static pages and scripts as in section 2.
2. `/etc/for-ident/for-ident.env` from `deploy/for-ident.env.example`: the database passwords, mail (section 8).
   `sudo chown root:forident /etc/for-ident/for-ident.env && sudo chmod 640 /etc/for-ident/for-ident.env`
3. Unit: `sudo cp for-ident.service /etc/systemd/system/ && sudo systemctl daemon-reload && sudo systemctl enable --now for-ident`
4. `journalctl -u for-ident -f` until "Started Application in … seconds"; the lines "database …: schema version 1"
   show the migrations.

The unit runs the service with systemd hardening (read-only system, private `/tmp`, no access to home
directories). Uploaded files live in the private `/tmp` and are deleted with the session. The native InChI library of
MetFrag is unpacked to `/tmp/jna` (`-Djna.tmpdir`).

## 7. Reverse proxy (Caddy)

- Caddy on the host: the blocks of `deploy/caddy/Caddyfile.example` in `/etc/caddy/Caddyfile`, `systemctl reload caddy`.
- `caddy-docker-proxy`: `deploy/caddy/docker-compose.override.example.yml`, then `docker compose up -d`.

Both: certificate from Let's Encrypt (DNS of `<host>` must point to the server), uploads up to 20 MB, HSTS, health
check of the application (`/actuator/health`). During the move use the staging name first.

### Protection against password guessing (fail2ban)

The application logs every failed login with the client address (`LoginFailureLogger`, no user name). fail2ban
blocks an address after 10 failures within 10 minutes for one hour:

```
sudo cp deploy/fail2ban/for-ident.conf /etc/fail2ban/filter.d/for-ident.conf
sudo cp deploy/fail2ban/jail.d-for-ident.conf /etc/fail2ban/jail.d/for-ident.conf
sudo fail2ban-client -t && sudo systemctl reload fail2ban
sudo fail2ban-client status for-ident
```

With Caddy in Docker the ban acts in the chain `DOCKER-USER` (published ports bypass the INPUT chain); with Caddy
on the host remove the `action` line of the jail. Unban an address: `sudo fail2ban-client set for-ident unbanip <ip>`.

## 8. Mail (registration, activation, password reset)

The application sends through the SMTP server of an existing mailbox of the domain; SPF and DKIM are then
handled by the mail provider (check that the domain's SPF record includes the provider). In the environment file:

```
FI_MAIL_HOST=<smtp server of the provider>
FI_MAIL_PORT=587                FI_MAIL_STARTTLS=true      (or 465 with FI_MAIL_SSL=true)
FI_MAIL_SMTP_AUTH=true
FI_MAIL_USERNAME=<mailbox>      FI_MAIL_PASSWORD=<password of the mailbox>
FI_FROM_EMAIL_ADDRESS=<address of that mailbox>
FI_CONTACT_ADDRESSES=<admins who get registration and report mails>
```

Check: log in as administrator → User administration → **Send test mail** (to your own address; an error message
names the cause). Then register a test account and use "Forgot password" once.

## 9. Backups

```
sudo install -d -o postgres -m 700 /var/backups/for-ident
sudo cp for-ident-backup.service for-ident-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now for-ident-backup.timer
sudo systemctl start for-ident-backup.service && journalctl -u for-ident-backup    (first run)
```

Daily at 02:30 one dump per database in `/var/backups/for-ident/<date>`, kept 14 days. Copy them regularly off
the server (backup space of the provider); they contain personal data (user database). Restore one database:
`sudo -u postgres pg_restore --clean --if-exists --no-owner --role=forident -d <database> <file>.dump` (stop the
application first).

## 10. Update and rollback

```
sudo -u postgres bash /opt/for-ident/bin/backup-databases.sh      (always before an update)
sudo systemctl stop for-ident
sudo cp /opt/for-ident/de.hswt.fi.application.jar /opt/for-ident/de.hswt.fi.application-previous.jar
sudo cp de.hswt.fi.application.jar /opt/for-ident/
sudo systemctl start for-ident && journalctl -u for-ident -f
```

Rollback: stop, copy back the previous JAR, start. If the new version ran a schema migration (log line "… migrations
applied" > 0), restore the databases from the backup made before the update.

## 11. Monitoring and logs

- Health: `curl -s http://localhost:8080/actuator/health` → `{"status":"UP",…}` (also from outside via HTTPS; only
  the status, no details). Liveness/readiness: `/actuator/health/liveness`, `/actuator/health/readiness`. The
  databases are part of the check; the mail server is not.
- Logs: `journalctl -u for-ident` (retention of the journal). More detail for a while:
  `LOGGING_LEVEL_DE_HSWT_FI=DEBUG` in the environment file, restart.
- If searches hang: `journalctl -u for-ident | grep -i "native thread"` (thread limit of the server).

## 12. Checks after an installation or update

1. `dev/smoke-test.sh https://<host>`: UI, static pages, REST API, Swagger UI.
2. Health is `UP`; the journal shows the three "schema version" lines and no `ERROR` (except the known messages of
   the initialization profile).
3. In the browser: log in, **molecular screening with an MS/MS file (MetFrag)**: candidates with MS/MS scores and no
   "Error loading InChI native code" in the journal; Excel export; log out.
4. As administrator: version in the side bar, **Send test mail**.
5. Backup timer: `systemctl list-timers for-ident-backup.timer`.

## Changes compared with 1.x and 2.x

| 1.x | 2.x | 3.x |
|---|---|---|
| Java 8, `-javaagent:spring-instrument…` | Java 17, no agent | Java 21, no agent |
| Database passwords inside the JAR | `/etc/for-ident/for-ident.env` | same, plus mail login |
| Tables created by the initialization profile | same | Flyway migrations (baseline on existing databases) |
| Log file `~/.config/de.hswt.fi/logs/fi-lc-production.log` | same | journal (`logback-console.xml`) |
| MongoDB client always started | same | only with `FI_PATHWAY_ENABLED=true` |
| — | — | `/actuator/health`, Swagger UI `/api/docs` |
