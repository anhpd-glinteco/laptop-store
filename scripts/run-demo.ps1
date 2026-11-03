$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Can cai JDK 17+" }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "Can cai Maven 3.6.3+ va them vao PATH" }
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
