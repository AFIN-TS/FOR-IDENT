<#
.SYNOPSIS
    Starts the built application JAR locally against the databases from setup-local-db.ps1.

.DESCRIPTION
    Sets the FI_DB_* environment variables for all three LC databases (URL, user and password
    explicitly, so the defaults of the active profiles do not matter) and starts
    application/build/libs/de.hswt.fi.application.jar with Java 21 (the JPA entities are woven at
    build time, no -javaagent needed).

    -Init adds the profile initialization-lc: creates the schema of the user database,
    the groups and the user "admin" (password FI_ADMIN_PASSWORD and optional mail FI_ADMIN_MAIL from the secrets file).
    Run it once with -Init, stop it, then start normally.

.EXAMPLE
    .\dev\run-local.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env -Init
    .\dev\run-local.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env
    .\dev\run-local.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env -HtmlDir D:\Claude\FOR-IDENT\dev-env\html
#>
param(
    [string]$SecretsFile = (Join-Path $PSScriptRoot "local.env"),
    [string]$Profiles = "lc",
    [switch]$Init,
    [int]$Port = 8080,
    # JDK 21 (3.x); default: the Temurin 21 that the Gradle toolchain download put into the Gradle user home
    [string]$JavaHome = (Join-Path $env:USERPROFILE ".gradle\jdks\eclipse_adoptium-21-amd64-windows.2"),
    [string]$LogFile,
    # Folder with the static pages of the home and help views (home_de.html, processing_help_en.html, ...),
    # served from the root URL. Default of the application: <user home>\html
    [string]$HtmlDir,
    # Local UI tests: account test-admin@example.test with ADMIN role and a new random password on every start; the
    # password is written to local-test-admin.env next to the secrets file (outside the repository). Also logs the
    # password reset links (no mail server locally). Never use it on a server.
    [switch]$TestAdmin
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot "local-env.ps1")

$secrets = Read-LocalSecrets $SecretsFile
Set-LocalDatabaseEnvironment $secrets

if ($Init) {
    if (-not $secrets.FI_ADMIN_PASSWORD) { throw "FI_ADMIN_PASSWORD is missing in $SecretsFile" }
    $env:FI_ADMIN_PASSWORD = $secrets.FI_ADMIN_PASSWORD
    # Required since 3.0.2; locally a reserved test domain is enough
    $env:FI_ADMIN_MAIL = if ($secrets.FI_ADMIN_MAIL) { $secrets.FI_ADMIN_MAIL } else { "admin@example.test" }
    $Profiles = "$Profiles,initialization-lc"
}

if ($TestAdmin) {
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $env:FI_TEST_ADMIN_PASSWORD = [Convert]::ToBase64String($bytes).Replace("+", "x").Replace("/", "y")
    $testAdminFile = Join-Path (Split-Path (Resolve-Path $SecretsFile)) "local-test-admin.env"
    "TEST_ADMIN_USERNAME=test-admin@example.test`nTEST_ADMIN_PASSWORD=$env:FI_TEST_ADMIN_PASSWORD" |
        Set-Content -Encoding ascii $testAdminFile
    $Profiles = "$Profiles,local-test-admin"
    Write-Host "Test admin test-admin@example.test, password in $testAdminFile"
}

$jar = Join-Path $root "application\build\libs\de.hswt.fi.application.jar"
if (-not (Test-Path $jar)) { throw "$jar not found, run gradlew build first" }

$javaArgs = @("-jar", $jar, "--spring.profiles.active=$Profiles", "--server.port=$Port")
if ($LogFile) { $javaArgs += "--logging.file.name=$LogFile" }
if ($TestAdmin) { $javaArgs += "--de.hswt.fi.security.log-reset-links=true" }
if ($HtmlDir) {
    $htmlUrl = (Resolve-Path $HtmlDir).Path.Replace('\', '/')
    $javaArgs += "--spring.web.resources.static-locations=file:///$htmlUrl/"
}

Write-Host "Starting with profiles $Profiles on http://localhost:$Port"
# Windows PowerShell treats every stderr line of a native program as an error; with "Stop" the first one would end
# the script and with it the application
$ErrorActionPreference = "Continue"
& "$JavaHome\bin\java.exe" @javaArgs
