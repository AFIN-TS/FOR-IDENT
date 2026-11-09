<#
.SYNOPSIS
    Runs the regression tests (application/src/regressionTest) against the local databases.

.DESCRIPTION
    The tests start the complete application with the profiles "lc" and compare import,
    processing, Excel export and REST API results with the golden files in
    application/src/regressionTest/resources/golden. Differing results are written to
    application/build/regression/actual.

    -Update rewrites the golden files with the current results. Only use it deliberately,
    e.g. after an accepted change of the scientific results, and review the Git diff.

.EXAMPLE
    .\dev\run-regression.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env
    .\dev\run-regression.ps1 -SecretsFile D:\Claude\FOR-IDENT\dev-env\local-secrets.env -Tests "*ImportRegressionTest"
#>
param(
    [string]$SecretsFile = (Join-Path $PSScriptRoot "local.env"),
    [switch]$Update,
    [string]$Tests,
    # Compare the REST API responses also with a production instance, e.g. https://for-ident.example.org
    [string]$ProductionUrl,
    # JDK that runs Gradle (11+); the tests themselves run on the Java 8 toolchain
    # JDK 21 to run Gradle: the Vaadin Gradle plugin (SP-11) needs it. Default: the Temurin 21 of the Gradle toolchain
    [string]$JavaHome = (Join-Path $env:USERPROFILE ".gradle\jdks\eclipse_adoptium-21-amd64-windows.2")
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot "local-env.ps1")

Set-LocalDatabaseEnvironment (Read-LocalSecrets $SecretsFile)
$env:JAVA_HOME = $JavaHome

# A fresh Gradle process: a long-running daemon can run out of memory during the long test run.
# -p: the script can be started from any directory
$gradleArgs = @("-p", $root, ":application:regressionTest", "--console=plain", "--no-daemon")
if ($Update) { $gradleArgs += "-Dregression.update=true" }
if ($Tests) { $gradleArgs += @("--tests", $Tests) }
if ($ProductionUrl) { $gradleArgs += "-Dregression.productionUrl=$ProductionUrl" }

# Gradle writes to stderr; with "Stop" Windows PowerShell 5.1 would abort on the first line
$ErrorActionPreference = "Continue"
& (Join-Path $root "gradlew.bat") @gradleArgs 2>&1 | ForEach-Object { "$_" }
exit $LASTEXITCODE
