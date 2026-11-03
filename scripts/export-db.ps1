param([string]$User="laptop_app",[string]$Database="laptop_store",[int]$Port=3306)
$ErrorActionPreference="Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$out = Join-Path (Get-Location) "database\laptop_store_export.sql"
# --result-file avoids PowerShell 5 UTF-16 redirection. Password is prompted, not saved.
mysqldump --host=127.0.0.1 "--port=$Port" "--user=$User" -p --single-transaction --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 "--result-file=$out" $Database
if ($LASTEXITCODE -ne 0) { throw "Export failed. Check connection and mysqldump PATH." }
Write-Host "Exported: $out"
