param(
    [Parameter(Mandatory = $true)][string]$JsonPath,
    [string]$SiteCode = 'default'
)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourcePath = Join-Path $scriptRoot 'NewsBlogDemoSeed.java'
$classesPath = Join-Path $env:TEMP 'cms-news-blog-seed-classes'
$postgresJar = Join-Path $env:USERPROFILE '.m2\repository\org\postgresql\postgresql\42.5.4\postgresql-42.5.4.jar'
$gsonJar = Join-Path $env:USERPROFILE '.m2\repository\com\google\code\gson\gson\2.13.2\gson-2.13.2.jar'

if (!(Test-Path -LiteralPath $JsonPath)) { throw "JSON file not found: $JsonPath" }
if (!(Test-Path -LiteralPath $sourcePath)) { throw "Seed source not found: $sourcePath" }
if (!(Test-Path -LiteralPath $postgresJar) -or !(Test-Path -LiteralPath $gsonJar)) { throw 'Required local JDBC or Gson dependency is missing.' }

New-Item -ItemType Directory -Force -Path $classesPath | Out-Null
javac -cp "$postgresJar;$gsonJar" -d $classesPath $sourcePath
if ($LASTEXITCODE -ne 0) { throw 'Seed source compilation failed.' }
java -cp "$classesPath;$postgresJar;$gsonJar" NewsBlogDemoSeed $JsonPath $SiteCode
if ($LASTEXITCODE -ne 0) { throw 'Seed execution failed.' }
