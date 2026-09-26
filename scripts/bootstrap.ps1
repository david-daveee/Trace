$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$taskTools = Join-Path $root '.tools'
New-Item -ItemType Directory -Force $taskTools | Out-Null
$gradleZip = Join-Path $taskTools 'gradle.zip'
if (!(Test-Path (Join-Path $taskTools 'gradle-8.11.1/bin/gradle.bat'))) {
    Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip' -OutFile $gradleZip
    Expand-Archive -LiteralPath $gradleZip -DestinationPath $taskTools -Force
}
Invoke-WebRequest 'https://repo.maven.apache.org/maven2/net/sourceforge/jexcelapi/jxl/2.6.12/jxl-2.6.12.jar' -OutFile (Join-Path $taskTools 'jxl.jar')
$sdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
('sdk.dir=' + ($sdk -replace '\\','/')) | Set-Content (Join-Path $root 'local.properties') -Encoding ascii
if (!(Test-Path (Join-Path $taskTools 'jdk'))) {
    Invoke-WebRequest 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip' -OutFile (Join-Path $taskTools 'jdk.zip')
    Expand-Archive -LiteralPath (Join-Path $taskTools 'jdk.zip') -DestinationPath (Join-Path $taskTools 'jdk') -Force
}
$env:JAVA_HOME = (Get-ChildItem (Join-Path $taskTools 'jdk') -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path $taskTools 'gradle-home'
& (Join-Path $taskTools 'gradle-8.11.1/bin/gradle.bat') -p $root wrapper --gradle-version 8.11.1 --distribution-type bin
if ($LASTEXITCODE -ne 0) { throw 'Gradle wrapper failed' }
