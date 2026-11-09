# Shared helpers of the dev scripts: reads the secrets file and sets the FI_DB_* environment
# variables for the three local LC databases (URL, user and password explicitly, so the defaults
# of the active Spring profiles do not matter).

function Read-LocalSecrets([string]$SecretsFile) {
    $secrets = @{}
    Get-Content $SecretsFile | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
        $key, $value = $_ -split '=', 2
        $secrets[$key] = $value
    }
    if (-not $secrets.FI_DB_APP_PASSWORD) { throw "FI_DB_APP_PASSWORD is missing in $SecretsFile" }
    return $secrets
}

function Set-LocalDatabaseEnvironment($Secrets) {
    $databases = @{
        USER        = "fi-lc-users"
        STOFFIDENT  = "fi-lc-stoffident"
        PLANTIDENT  = "fi-lc-plantident"
    }
    foreach ($source in $databases.Keys) {
        Set-Item "Env:\FI_DB_${source}_URL" "jdbc:postgresql://localhost:5432/$($databases[$source])"
        Set-Item "Env:\FI_DB_${source}_USERNAME" "forident"
        Set-Item "Env:\FI_DB_${source}_PASSWORD" $Secrets.FI_DB_APP_PASSWORD
    }
}
