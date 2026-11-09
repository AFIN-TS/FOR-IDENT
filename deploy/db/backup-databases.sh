#!/usr/bin/env bash
# Daily backup of the FOR-IDENT databases (SP-16), run as user postgres by for-ident-backup.timer:
# one pg_dump per database (custom format) in BACKUP_DIR/<date>, older backups are deleted after KEEP_DAYS.
# The user database contains personal data: the backups are readable only by postgres. Copy them off the server
# (e.g. the provider's backup space) as described in deploy/README.md.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/for-ident}"
KEEP_DAYS="${KEEP_DAYS:-14}"
DATABASES="${DATABASES:-fi-lc-users fi-lc-stoffident fi-lc-plantident}"

umask 077
target="$BACKUP_DIR/$(date +%Y-%m-%d)"
mkdir -p "$target"
for db in $DATABASES; do
	pg_dump --format=custom --file="$target/$db.dump" "$db"
done
(cd "$target" && sha256sum ./*.dump > SHA256SUMS)
echo "backup written to $target: $(du -sh "$target" | cut -f1)"

# Delete backups older than KEEP_DAYS days (only the date directories of this script)
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20[0-9][0-9]-[0-9][0-9]-[0-9][0-9]' -mtime +"$KEEP_DAYS" \
	-print -exec rm -r {} +
