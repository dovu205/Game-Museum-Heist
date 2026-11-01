param([Parameter(Mandatory=$true)][string]$ServerHost,[ValidateRange(1,65535)][int]$Port=5555)
$ErrorActionPreference='Stop'
if($ServerHost -notmatch '^[A-Za-z0-9.:-]+$'){throw 'ServerHost must be an IP address or hostname.'}
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
 $compiled=Join-Path $projectRoot ('build\client-'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $compiled -Force | Out-Null
 foreach($module in @('shared','client')){
  $sources=@(Get-ChildItem -LiteralPath "$module/src/main/java" -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
  & javac -encoding UTF-8 --release 17 -cp $compiled -d $compiled @sources
  if($LASTEXITCODE -ne 0){throw "Build failed: $module"}
 }
 $clientDirectory=Join-Path $projectRoot 'dist\MuseumHeist-Client'
 New-Item -ItemType Directory -Path $clientDirectory -Force | Out-Null
 $clientResources=Join-Path $projectRoot ('build\client-config-'+[guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $clientResources -Force | Out-Null
 Copy-Item -Path 'client/src/main/resources/*' -Destination $clientResources -Recurse -Force
 [IO.File]::WriteAllText((Join-Path $clientResources 'client.properties'),"server.host=$ServerHost`nserver.port=$Port`n",[Text.UTF8Encoding]::new($false))
 & jar --create --file "$clientDirectory/MuseumHeist-Client.jar" --main-class vn.edu.policethief.client.ClientMain -C $compiled . -C $clientResources .
 if($LASTEXITCODE -ne 0){throw 'Client packaging failed.'}
 $launcher=@"
@echo off
setlocal
cd /d "%~dp0"
set "SERVER_IP=$ServerHost"
set "SERVER_PORT=$Port"
java -Dfile.encoding=UTF-8 -Dnightchase.host=%SERVER_IP% -Dnightchase.port=%SERVER_PORT% -jar MuseumHeist-Client.jar
"@
 [IO.File]::WriteAllText((Join-Path $clientDirectory 'ChoiGame.bat'),($launcher -replace "`r?`n","`r`n"),[Text.Encoding]::ASCII)
 $instructions=@"
# Cảnh sát và Trộm — Client phiên bản 2

Giải nén toàn bộ ZIP, dùng Java 17 trở lên, mở ChoiGame.bat.
Server đã cấu hình: ${ServerHost}:$Port. Đổi IP qua Kết nối phòng chơi trên màn hình đăng nhập nếu cần.
Hai máy phải cùng phiên bản 2 (32 nhịp, 8 giây, bẫy và luồn lách).
Máy chủ chạy run-server.bat hoặc May1-ChuPhong.bat trong bộ đầy đủ.
Không chứa server, SQLite hoặc dữ liệu tài khoản. Tài khoản và lịch sử lưu trên máy chủ.
Xem Luật chơi trong game. Hai máy cần kết nối được cổng TCP $Port.
"@
 [IO.File]::WriteAllText((Join-Path $clientDirectory 'HUONG_DAN.md'),$instructions,[Text.UTF8Encoding]::new($false))
 $allowed=@('MuseumHeist-Client.jar','ChoiGame.bat','HUONG_DAN.md')
 if(Get-ChildItem -LiteralPath $clientDirectory -Force | Where-Object {$_.Name -notin $allowed}){throw 'Unexpected files in client output; refusing to package private data.'}
 Compress-Archive -LiteralPath $clientDirectory -DestinationPath 'dist/MuseumHeist-Client.zip' -Force
 Write-Output "Client ready: $projectRoot/dist/MuseumHeist-Client.zip ($ServerHost`:$Port)"
} finally {Pop-Location}
