param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Tasks)
$ErrorActionPreference='Stop'
$rootPath=Split-Path -Parent $PSScriptRoot
$javaDirectory=Get-ChildItem -LiteralPath (Join-Path $rootPath '.deps\java') -Directory | Select-Object -First 1
$env:JAVA_HOME=$javaDirectory.FullName
$env:GRADLE_USER_HOME=Join-Path $rootPath '.deps\gradle'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS='-Duser.language=en -Duser.country=US'
# ForgeGradle performs a network HEAD probe even in Gradle offline mode. Disable
# that preflight only for offline tasks; HTTPS validation for downloads is unchanged.
if ($Tasks -contains '--offline') { $env:JAVA_TOOL_OPTIONS += ' -Dnet.minecraftforge.gradle.check.certs=false' }
& (Join-Path $rootPath 'mod\gradlew.bat') -p (Join-Path $rootPath 'mod') @Tasks --console=plain
exit $LASTEXITCODE
