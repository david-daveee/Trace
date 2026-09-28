param([string[]]$Tasks = @('assembleDebug','testDebugUnitTest','lintDebug'))
$ErrorActionPreference='Stop'
$Tasks=$Tasks -split ','
$root=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME=(Get-ChildItem (Join-Path $root '.tools/jdk') -Directory | Select-Object -First 1).FullName
$taskCacheLink=Join-Path $env:TEMP 'podhod-gradle-cache'
if (!(Test-Path $taskCacheLink)) { New-Item -ItemType Junction -Path $taskCacheLink -Target (Join-Path $root '.tools/gradle-home') | Out-Null }
$env:GRADLE_USER_HOME=$taskCacheLink
$env:ANDROID_HOME=Join-Path $env:LOCALAPPDATA 'Android/Sdk'
('sdk.dir=' + (($env:ANDROID_HOME -replace '\\','/') -replace ':','\:')) | Set-Content (Join-Path $root 'local.properties') -Encoding ascii
$taskProjectLink=Join-Path $env:TEMP 'podhod-project'
if (!(Test-Path $taskProjectLink)) { New-Item -ItemType Junction -Path $taskProjectLink -Target $root | Out-Null }
$taskStudioJava=Join-Path (Join-Path $taskProjectLink '.tools/jdk') (Split-Path $env:JAVA_HOME -Leaf)
New-Item -ItemType Directory -Force (Join-Path $root '.gradle') | Out-Null
('java.home=' + ($taskStudioJava -replace '\\','/')) | Set-Content (Join-Path $root '.gradle/config.properties') -Encoding ascii
& (Join-Path $taskProjectLink '.tools/gradle-8.11.1/bin/gradle.bat') -p $taskProjectLink @Tasks --console=plain
exit $LASTEXITCODE
