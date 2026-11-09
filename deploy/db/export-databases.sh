#!/usr/bin/env bash
# Export of the FOR-IDENT databases for the move to a new server (SP-16), run on the OLD server:
#
#   sudo -u postgres bash export-databases.sh /var/tmp/fi-db-export
#
# Writes one dump per database (pg_dump custom format, readable by newer pg_restore, e.g. PostgreSQL 12 -> 17),
# the row count of every table (to check the import) and SHA-256 checksums. The user database contains personal
# data: the directory is readable only by its owner; copy it with scp and delete it after the import.
set -euo pipefail

TARGET_DIR="${1:?usage: export-databases.sh <target directory>}"
DATABASES="${DATABASES:-fi-lc-users fi-lc-stoffident fi-lc-plantident}"

umask 077
mkdir -p "$TARGET_DIR"
cd "$TARGET_DIR"

for db in $DATABASES; do
	echo "== $db"
	pg_dump --format=custom --no-owner --no-privileges --file="$db.dump" "$db"
	# Exact row count of every table, as "table<TAB>rows", sorted
	psql --no-psqlrc --tuples-only --no-align --field-separator=$'\t' --dbname="$db" --command="
		select c.relname,
		       (xpath('/row/n/text()', query_to_xml(format('select count(*) as n from %I.%I', n.nspname, c.relname),
		        false, true, '')))[1]::text
		from pg_class c join pg_namespace n on n.oid = c.relnamespace
		where c.relkind = 'r' and n.nspname = 'public' order by 1" > "$db.counts"
	echo "   $(wc -l < "$db.counts") tables, $(du -h "$db.dump" | cut -f1)"
done

sha256sum ./*.dump ./*.counts > SHA256SUMS
echo "Export written to $TARGET_DIR (checksums in SHA256SUMS)."
echo "PostgreSQL version: $(psql --no-psqlrc --tuples-only --no-align --command='show server_version')"
