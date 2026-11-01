# Kiến trúc

Project Maven `vn.edu:police-thief-java:2.0.0`, Java 17 trở lên. Thư mục gốc hiện tại được giữ tại `Game_Police_vs_Thief` để các file mở game và dữ liệu hiện có tiếp tục dùng được.

## Module shared

`vn.edu.policethief.shared`: `GameRules` chứa hằng số, hình học và kiểm tra chung cho preview/server. `model` chứa Role, Position, Direction, ActionType, PlayerAction, TileType và GameState. GameState là cấu trúc bàn cờ đầy đủ; client chỉ dùng một bản tạm cho kiểm tra hành động của chính mình, không nhận GameState đầy đủ qua mạng. `protocol` chứa Packet và PacketType.

## Module server

- `ServerMain`: chạy TCP độc lập. `HostMain`: tùy chọn tương thích file mở chủ phòng, khởi động server rồi gọi giao diện bằng reflection khi client có trên classpath.
- `network/TcpServer`, `ClientHandler`: TCP, xác thực, điều phối gói tin, hàng đợi gửi có giới hạn, heartbeat và ngắt kết nối.
- `lobby/Room`, `RoomManager`: lời mời, trận hai người, hai ván đổi vai, điểm, đầu hàng và chơi lại.
- `game/GameSession`: bàn cờ thật; `TurnManager`: thời hạn 8 giây, xử lý khi đủ hành động, đứng yên nếu thiếu.
- `ActionValidator`: server kiểm tra lại hành động; `TurnResolver`: xử lý đồng thời từng bước.
- `MapGenerator`: kiểm tra kết nối bản đồ và chọn bốn vị trí khác nhau; bản đồ thiết kế nằm tại `resources/maps/map01.txt`, đồng bộ hình học trong shared.
- `TrapService`: đặt, kích hoạt và lọc bẫy; `VisibilityService`: tầm nhìn, phát hiện qua bẫy và tình báo; `WinConditionService`: bắt, thoát, hết 32 nhịp.
- `Store`: SQLite, tài khoản, PBKDF2, điểm, lịch sử, phân trang, transaction và nhập dữ liệu cũ. `resources/db/schema.sql` là schema.

Mọi thay đổi gameplay được tuần tự hóa dưới monitor của TcpServer. Không nhận tọa độ từ client; chỉ nhận hành động và định danh trận/ván/nhịp. Room lọc dữ liệu riêng theo vai trước khi gửi STATE/UPDATE. Đối thủ không nhận lựa chọn đang chờ hoặc đường di chuyển bí mật.

## Module client

`ClientMain` → `network/GameClient` → `controller/GameController` → `ui/MainFrame`. Các panel chính: LobbyPanel, GamePanel, BoardPanel, ActionPanel. Giữ các màn đăng ký/đăng nhập, cấu hình IP, âm lượng, bảng xếp hạng và lịch sử. Không có phụ thuộc production từ client vào server; phụ thuộc test giúp chạy thử client thật với TCP server.

Swing chỉ cập nhật trạng thái trên EDT. Các phím hướng, Space và Enter được gắn cả tại thành phần đang focus, tránh ô nhật ký/vùng cuộn chặn điều khiển sau đổi nhịp. Bẫy chưa kích hoạt không được gửi cho trộm. Cảnh sát không nhận trạng thái báu vật bị lấy, vật mang theo hoặc số lượt luồn lách.

## Build và dữ liệu

`mvn package` tạo JAR ba module và chạy suite kiểm thử. `build.bat` dùng JDK để tạo bộ chạy đầy đủ `MuseumHeist.jar` cùng hai JAR chạy độc lập ở `build/`, không cần tải thư viện để build. SQLite JDBC có sẵn trong `lib/`; Maven dùng đúng bản đi kèm. `test.bat` chạy toàn bộ test bằng JDK, không cần JUnit bên ngoài.

Giữ nguyên thư mục `data/` hiện có. Test dùng thư mục tạm `work/`, không dùng dữ liệu thật. Những tài liệu kiểm thử cũ trong docs là báo cáo lịch sử, không mô tả luật phiên bản 2; xem `rules.md`, `protocol.md` và `verification.md`.
