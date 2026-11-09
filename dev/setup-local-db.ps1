<#
.SYNOPSIS
    Creates the local FOR-IDENT databases and restores the compound database dumps.

.DESCRIPTION
    Creates the role "forident" and the databases fi-lc-users, fi-lc-stoffident,
    and fi-lc-plantident on a local PostgreSQL
    (localhost:5432), then restores the compound databases from pg_dump custom-format
    files (<database>.dump). The user database stays empty; run-local.ps1 -Init creates
    its schema and the admin user.

.EXAMPLE
    .\dev\setup-local-db.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env -DumpDir D:\Claude\FOR-IDENT\db-dumps\2026-10-29
#>
param(
    [string]$SecretsFile = (Join-Path $PSScriptRoot "local.env"),
    [Parameter(Mandatory = $true)][string]$DumpDir,
    [string]$PgBin = "C:\Program Files\PostgreSQL\17\bin"
)

$ErrorActionPreference = "Stop"

$secrets = @{}
Get-Content $SecretsFile | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $key, $value = $_ -split '=', 2
    $secrets[$key] = $value
}
foreach ($key in "PG_SUPERUSER_PASSWORD", "FI_DB_APP_PASSWORD") {
    if (-not $secrets[$key]) { throw "$key is missing in $SecretsFile" }
}

$compoundDatabases = "fi-lc-stoffident", "fi-lc-plantident"

$env:PGPASSWORD = $secrets.PG_SUPERUSER_PASSWORD
$escapedPassword = $secrets.FI_DB_APP_PASSWORD.Replace("'", "''")
& "$PgBin\psql.exe" -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -qc "CREATE ROLE forident LOGIN PASSWORD '$escapedPassword'"
foreach ($db in @("fi-lc-users") + $compoundDatabases) {
    & "$PgBin\createdb.exe" -h localhost -U postgres -O forident -E UTF8 $db
    if ($LASTEXITCODE -ne 0) { throw "createdb $db failed" }
}

$env:PGPASSWORD = $secrets.FI_DB_APP_PASSWORD
foreach ($db in $compoundDatabases) {
    & "$PgBin\pg_restore.exe" -h localhost -U forident -d $db --no-owner --no-privileges --exit-on-error (Join-Path $DumpDir "$db.dump")
    if ($LASTEXITCODE -ne 0) { throw "pg_restore $db failed" }
    $count = & "$PgBin\psql.exe" -h localhost -U forident -d $db -Atc "select count(*) from tbl_substances"
    Write-Host "$db restored, $count substances"
}
Remove-Item Env:\PGPASSWORD
