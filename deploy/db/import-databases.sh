#!/usr/bin/env bash
# Import of the FOR-IDENT databases on the NEW server (SP-16), from the directory written by export-databases.sh:
#
#   sudo -u postgres bash import-databases.sh /var/tmp/fi-db-export
#
# Checks the checksums, creates the role forident (if missing, without password: set it with \password forident,
# SCRAM-SHA-256) and the databases owned by forident, restores the dumps and compares the row count of every table
# with the export. Stops with an error if a database already exists (no accidental overwrite) or a count differs.
#
# TARGET_SUFFIX (optional, for a trial run): restore into "<database><suffix>" instead of the original names.
set -euo pipefail

SOURCE_DIR="${1:?usage: import-databases.sh <export directory>}"
DATABASES="${DATABASES:-fi-lc-users fi-lc-stoffident fi-lc-plantident}"
APP_ROLE="${APP_ROLE:-forident}"
TARGET_SUFFIX="${TARGET_SUFFIX:-}"

cd "$SOURCE_DIR"
sha256sum --check --quiet SHA256SUMS
echo "Checksums OK."

psql_cmd() {
	psql --no-psqlrc --tuples-only --no-align --set=ON_ERROR_STOP=1 "$@"
}

if [ -z "$(psql_cmd --dbname=postgres --command="select 1 from pg_roles where rolname = '$APP_ROLE'")" ]; then
	psql_cmd --dbname=postgres --command="create role \"$APP_ROLE\" login"
	echo "Role $APP_ROLE created; set its password: sudo -u postgres psql -c '\\password $APP_ROLE'"
fi

failed=0
for db in $DATABASES; do
	target="$db$TARGET_SUFFIX"
	echo "== $db -> $target"
	if [ -n "$(psql_cmd --dbname=postgres --command="select 1 from pg_database where datname = '$target'")" ]; then
		echo "   database $target exists already - stopped (drop it first if it is to be replaced)" >&2
		exit 1
	fi
	psql_cmd --dbname=postgres --command="create database \"$target\" owner \"$APP_ROLE\" encoding 'UTF8' template template0"
	pg_restore --no-owner --no-privileges --role="$APP_ROLE" --exit-on-error --dbname="$target" "$db.dump"
	psql_cmd --dbname="$target" --command="analyze" > /dev/null

	psql_cmd --field-separator=$'\t' --dbname="$target" --command="
		select c.relname,
		       (xpath('/row/n/text()', query_to_xml(format('select count(*) as n from %I.%I', n.nspname, c.relname),
		        false, true, '')))[1]::text
		from pg_class c join pg_namespace n on n.oid = c.relnamespace
		where c.relkind = 'r' and n.nspname = 'public' order by 1" > "/tmp/$target.counts.$$"
	if diff -u "$db.counts" "/tmp/$target.counts.$$"; then
		echo "   $(wc -l < "$db.counts") tables, all row counts equal"
	else
		echo "   ROW COUNTS DIFFER (see above)" >&2
		failed=1
	fi
	rm -f "/tmp/$target.counts.$$"
done

if [ "$failed" -ne 0 ]; then
	echo "Import finished WITH DIFFERENCES - check before starting FOR-IDENT." >&2
	exit 1
fi
echo "Import finished, all row counts equal. Delete the export directory (personal data): rm -r $SOURCE_DIR"
echo "On its first start FOR-IDENT records the schema as Flyway version 1 (baseline) in each database."
