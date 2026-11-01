param([switch]$Tests)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
 $output=Join-Path $projectRoot ('build\app-'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $output -Force | Out-Null
 foreach($module in @('shared','server','client')){
  $sources=@(Get-ChildItem -LiteralPath "$module/src/main/java" -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
  & javac -encoding UTF-8 --release 17 -cp $output -d $output @sources
  if($LASTEXITCODE -ne 0){throw "Compilation failed: $module"}
 }
 $manifest=Join-Path $projectRoot 'build\MANIFEST.MF'
 [IO.File]::WriteAllText($manifest,"Class-Path: lib/sqlite-jdbc-3.53.4.0.jar`n",[Text.Encoding]::ASCII)
 & jar --create --file MuseumHeist.jar --main-class vn.edu.policethief.server.HostMain --manifest $manifest -C $output . -C server/src/main/resources . -C client/src/main/resources .
 if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
 & jar --create --file build/police-thief-client.jar --main-class vn.edu.policethief.client.ClientMain -C $output vn/edu/policethief/client -C $output vn/edu/policethief/shared -C client/src/main/resources .
 if($LASTEXITCODE -ne 0){throw 'Client packaging failed'}
 & jar --create --file build/police-thief-server.jar --main-class vn.edu.policethief.server.ServerMain -C $output vn/edu/policethief/server -C $output vn/edu/policethief/shared -C server/src/main/resources .
 if($LASTEXITCODE -ne 0){throw 'Server packaging failed'}
 if($Tests){
  $testOutput=Join-Path $projectRoot 'build/tests'
  New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
  Copy-Item -Path 'server/src/test/resources/*' -Destination $testOutput -Recurse -Force
  $testSources=@(Get-ChildItem -Path 'server/src/test/java','client/src/test/java' -Recurse -Filter '*.java' | Where-Object {$_.Name -notlike '*Suite.java'} | ForEach-Object {$_.FullName})
  & javac -encoding UTF-8 --release 17 -cp MuseumHeist.jar -d $testOutput @testSources
  if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
  foreach($test in @('server.game.EngineTest','server.network.StoreTest','server.network.NetworkTest','server.network.NewRulesNetworkTest','server.network.MultiClientTest','server.network.PaginationTest','client.ClientConfigTest','client.ui.UiTest','client.ui.ClientFlowTest')){
   & java '-Dfile.encoding=UTF-8' -cp "MuseumHeist.jar;$testOutput" "vn.edu.policethief.$test"
   if($LASTEXITCODE -ne 0){throw "Test failed: $test"}
  }
 }
 Write-Output 'Build OK: MuseumHeist.jar, build/police-thief-client.jar, build/police-thief-server.jar'
} finally {Pop-Location}
