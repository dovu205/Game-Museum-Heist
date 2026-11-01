param(
 [Parameter(Mandatory=$true)][string]$ServerHost,
 [ValidateRange(1,65535)][int]$Port=5555
)
$ErrorActionPreference='Stop'
if($ServerHost -notmatch '^[A-Za-z0-9.:-]+$'){throw 'ServerHost must be an IP address or hostname.'}
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
 $compiled=Join-Path $projectRoot 'build\client-app'
 New-Item -ItemType Directory -Path $compiled -Force | Out-Null
 $sources=@(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\nightchase') -Filter '*.java' | ForEach-Object {$_.FullName})
 & javac -encoding UTF-8 --release 17 -d $compiled @sources
 if($LASTEXITCODE -ne 0){throw 'Build failed.'}
 # Do not rebuild or replace the main JAR while a host may be playing.
 [IO.File]::WriteAllText((Join-Path $projectRoot 'build\client.properties'),"server.host=$ServerHost`nserver.port=$Port`n",[Text.UTF8Encoding]::new($false))
 $clientDirectory=Join-Path $projectRoot 'dist\MuseumHeist-Client'
 New-Item -ItemType Directory -Path $clientDirectory -Force | Out-Null
 $clientJar=Join-Path $clientDirectory 'MuseumHeist-Client.jar'
 # Direct Client entry point: --host cannot invoke Launcher or open a server.
 & jar "-J-Djava.io.tmpdir=$projectRoot\build" --create --file $clientJar --main-class nightchase.Client -C $compiled nightchase -C assets audio -C build client.properties
 if($LASTEXITCODE -ne 0){throw 'Client packaging failed.'}
 $launcher=@"
@echo off
setlocal
cd /d "%~dp0"
rem Change this address if the host's Wi-Fi/LAN IP changes.
set "SERVER_IP=$ServerHost"
set "SERVER_PORT=$Port"
where javaw >nul 2>nul
if errorlevel 1 (
 echo Can cai Java 17 tro len va them Java vao PATH.
 pause
 exit /b 1
)
if not exist MuseumHeist-Client.jar (
 echo Hay giai nen toan bo file ZIP truoc khi mo game.
 pause
 exit /b 1
)
start "" javaw -Dfile.encoding=UTF-8 -Dnightchase.host=%SERVER_IP% -Dnightchase.port=%SERVER_PORT% -jar "%~dp0MuseumHeist-Client.jar"
"@
 [IO.File]::WriteAllText((Join-Path $clientDirectory 'ChoiGame.bat'),($launcher -replace "`r?`n","`r`n"),[Text.Encoding]::ASCII)
 $instructions=@"
# Museum Heist — Bản client

1. Giải nén toàn bộ ZIP trên máy người chơi thứ hai.
2. Cài Java 17 trở lên nếu máy chưa có Java, và thêm Java vào PATH.
3. Kết nối cùng mạng Wi-Fi/LAN với máy chủ phòng; không cần Radmin khi dùng cùng mạng nội bộ.
4. Mở ChoiGame.bat. IP máy chủ đã đặt sẵn: **${ServerHost}:$Port**.
5. Đăng nhập tài khoản cũ hoặc đăng ký tài khoản mới trên server.
6. Vào sảnh, thách đấu và chấp nhận lời mời.

Nếu IP thay đổi, chọn “Kết nối phòng chơi…” trên màn hình đăng nhập để đổi IP/cổng cho lần kết nối hiện tại, hoặc sửa SERVER_IP/SERVER_PORT trong ChoiGame.bat để dùng cho các lần mở sau.

Cả ChoiGame.bat và mở trực tiếp JAR đều dùng IP máy chủ đã cấu hình **${ServerHost}:$Port**. Nếu cần đổi IP khi mở JAR, chọn “Kết nối phòng chơi…”.

Đây là bản client: không khởi động server và không chứa database, tài khoản, thư viện SQLite hay dữ liệu của máy chủ. Tài khoản, điểm và lịch sử được lưu tập trung trên máy chủ. Máy chủ phải giữ game chủ phòng mở trong suốt trận.

Máy chủ dùng May1-ChuPhong.bat trong bộ đầy đủ, cổng TCP $Port. Hai máy cần dùng cùng phiên bản game. Nếu kết nối không được, kiểm tra cùng mạng Wi-Fi/LAN, IP và quyền Java/TCP $Port trong Windows Firewall của máy chủ. Mạng khách hoặc tính năng cách ly thiết bị trên router có thể chặn kết nối giữa hai máy.
"@
 [IO.File]::WriteAllText((Join-Path $clientDirectory 'HUONG_DAN.md'),$instructions,[Text.UTF8Encoding]::new($false))
 $allowed=@('MuseumHeist-Client.jar','ChoiGame.bat','HUONG_DAN.md')
 $unexpected=Get-ChildItem -LiteralPath $clientDirectory -Force | Where-Object {$_.Name -notin $allowed}
 if($unexpected){throw 'Unexpected files in client output; refusing to package possible private data.'}
 $zip=Join-Path $projectRoot 'dist\MuseumHeist-Client.zip'
 Compress-Archive -LiteralPath $clientDirectory -DestinationPath $zip -Force
 Write-Output "Client ready: $zip"
 Write-Output "Server: ${ServerHost}:$Port"
} finally {Pop-Location}
