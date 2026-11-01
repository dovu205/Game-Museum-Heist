# Cảnh sát và Trộm — police-thief-java 2.0

Game Java Swing, TCP Client–Server cho hai người. Luật mới: **32 nhịp/ván, 8 giây/nhịp, hai cửa A7/G1, cả hai vai chạy nhanh, cảnh sát đặt 2 bẫy, trộm luồn lách 3 lần**. Hai ván đổi vai, chọn hành động bí mật, xử lý đồng thời từng bước.

Giữ thư mục project `Game_Police_vs_Thief` và dữ liệu `data/`. Tên project Maven là `police-thief-java`.

## Chạy

Cần Java 17 trở lên, JDK nếu muốn build.

- **May1-ChuPhong.bat:** server kèm giao diện người chơi.
- **May2-ThamGia.bat:** máy khách; cấu hình IP tại “Kết nối phòng chơi…”, đăng ký/đăng nhập rồi thách đấu.
- **run-server.bat:** server riêng không giao diện; cổng mặc định 5555, hoặc `run-server.bat 5556`.
- **run-client.bat:** client riêng, không chứa server hay SQLite.

Hai máy dùng cùng bản phiên bản 2 và kết nối được TCP 5555. Chơi cùng máy dùng IP 127.0.0.1; chơi LAN dùng IP LAN máy chủ. Tài khoản/lịch sử lưu tại `data/museumheist.db` trên server.

## Build và kiểm thử

```bat
build.bat
test.bat
```

Build bằng JDK, không cần Maven hoặc tải thư viện. Test dùng dữ liệu riêng trong `work/`, cần desktop Swing và quyền kết nối TCP localhost. Kết quả build:

- `MuseumHeist.jar`: bộ đầy đủ cho các file mở game hiện có; thêm `--host` để làm chủ phòng.
- `build/police-thief-client.jar`: client + shared + tài nguyên.
- `build/police-thief-server.jar`: server + shared + bản đồ/schema; chạy với SQLite trong `lib/`.

Giữ `lib/sqlite-jdbc-3.53.4.0.jar` khi sao chép bộ đầy đủ.

Hoặc dùng Maven:

```bat
mvn package
```

Maven build ba module và chạy kiểm thử. JAR module nằm trong các thư mục `target/`, không gộp phụ thuộc. Các file `.bat` dùng bộ JAR đóng gói bởi `build.bat`. Mở `pom.xml` gốc trong IDE, chạy `vn.edu.policethief.server.ServerMain` và `vn.edu.policethief.client.ClientMain`.

Đóng gói client với IP máy chủ đặt sẵn (thay IP ví dụ bằng IP thực tế):

```powershell
powershell -ExecutionPolicy Bypass -File tools/package-client.ps1 -ServerHost 192.168.1.10 -Port 5555
```

Kết quả `dist/MuseumHeist-Client.zip`, không chứa dữ liệu tài khoản/server.

## Cấu trúc

```text
pom.xml
build.bat / test.bat / run-server.bat / run-client.bat
shared/
  pom.xml
  src/main/java/vn/edu/policethief/shared/
    GameRules.java
    model/Role, Position, Direction, ActionType, PlayerAction, TileType, GameState
    protocol/Packet, PacketType
server/
  pom.xml
  src/main/java/vn/edu/policethief/server/
    ServerMain.java / HostMain.java / Store.java
    network/TcpServer, ClientHandler
    lobby/Room, RoomManager
    game/GameSession, TurnManager, ActionValidator, TurnResolver,
         MapGenerator, TrapService, VisibilityService, WinConditionService
  src/main/resources/maps/map01.txt
  src/main/resources/db/schema.sql
  src/test/java/... / src/test/resources/fixtures/...
client/
  pom.xml
  src/main/java/vn/edu/policethief/client/
    ClientMain.java
    network/GameClient
    controller/GameController, ClientActions
    model/ClientState, OnlinePlayer
    ui/MainFrame, LobbyPanel, GamePanel, BoardPanel, ActionPanel, ...
    audio/Audio
  src/main/resources/images/, sounds/, rules.txt, client.properties
  src/test/java/...
docs/architecture.md / rules.md / protocol.md / verification.md
tools/ / lib/ / data/
```

Giữ đăng ký, đăng nhập, lời mời, bảng xếp hạng, lịch sử, âm thanh và chơi lại. Quét/Mồi nhử được thay bằng Đặt bẫy/Luồn lách. Phím hướng, Space và Enter điều khiển được khi focus ở nhật ký/vùng cuộn.

Tài liệu: [Luật chơi](docs/rules.md), [Kiến trúc](docs/architecture.md), [Gói tin](docs/protocol.md), [Kiểm thử](docs/verification.md).
