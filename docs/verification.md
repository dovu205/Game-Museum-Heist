# Kiểm thử phiên bản 2 — 01/11/2026

Đã chạy thành công `mvn -B -o package` trên Java 21 với target Java 17: ba module shared/server/client đều SUCCESS; 9 kiểm thử JUnit trong ServerSuite và ClientSuite, không lỗi hoặc bỏ qua.

Đã chạy thành công `tools/build.ps1 -Tests`, tương đương `test.bat`: build bộ đầy đủ và hai JAR độc lập rồi chạy test qua chính bộ JAR đóng gói.

| Bộ kiểm tra | Kết quả |
|---|---|
| EngineTest | 24.650 assertions luật mới, gồm 1.000 bản đồ và 1.000 ván ngẫu nhiên |
| StoreTest | 43 assertions nhập dữ liệu cũ, SQLite và transaction |
| NetworkTest | 43 assertions TCP, xử lý đủ hành động/hết 8 giây, 2 ván, đầu hàng, lưu lại khi lỗi |
| NewRulesNetworkTest | 18 assertions TCP về bẫy, giữ một nhịp, quyền xem thông tin, tầm nhìn, tình báo và chạy nhanh hai vai |
| MultiClientTest | 35 assertions, 8 tài khoản, 2 trận độc lập và 4 người chờ |
| PaginationTest | 400 assertions phân trang TCP |
| ClientConfigTest | Địa chỉ mặc định và cấu hình IP/cổng ghi đè đạt |
| UiTest | 126 assertions giao diện, luồn lách, bẫy, phím hướng/Enter khi nhật ký nhận focus |
| ClientFlowTest | 13 assertions đăng ký, đăng nhập, mời đấu, chơi, mất kết nối và đăng xuất qua TCP thật |

Engine bao phủ bắt cùng ô, đổi chỗ, bắt trong từng bước chạy/luồn lách, bắt ưu tiên thoát, thu thập báu vật đi ngang, dừng ngay khi thoát, hai cửa, hết nhịp 32, chạy nhanh của cảnh sát không gây tiếng động, bẫy không có hiệu lực ngay khi đặt, cấm vùng cửa kể cả góc, bẫy tồn tại và tái kích hoạt khi đi vào lại, đứng trên bẫy không bị giữ liên tục, tiếng động/dấu chân hết hạn và tình báo đúng nhịp.

Các test dùng database tạm trong `work/` của project/module, không dùng database thật trong `data/`. Stack trace có chuỗi `injected result write failure` là tình huống lỗi SQLite chủ động tạo để kiểm tra rollback và retry, sau đó test vẫn PASS.

Đã xuất và xem trực tiếp ảnh giao diện tại `work/preview-v2/`: hai cửa, preview đường luồn lách hình chữ L, bảng kỹ năng mới, biểu tượng bẫy, đối thủ được phát hiện, số nhịp/thời gian và màn hình thu gọn. Các PNG được tạo bằng Swing snapshot, không cần mạng.

Build Maven cần các plugin/JUnit trong cache hoặc Internet cho lần tải đầu. Build JDK dùng SQLite JDBC có sẵn và không cần tải phụ thuộc. Kiểm thử TCP cần localhost được cho phép; kiểm thử Swing cần môi trường desktop.
