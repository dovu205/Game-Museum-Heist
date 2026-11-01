> Tài liệu lịch sử của bản trước. Bản 2 xem [kiến trúc](architecture.md), [luật chơi](rules.md), [kiểm thử](verification.md).

# Kết quả kiểm thử — Museum Heist SQLite

## Kiểm tra bố trí ngẫu nhiên và cửa cố định — 01/11/2026

`test.bat` đạt toàn bộ: 43 kiểm tra SQLite, 7.788 luật chơi, 44 TCP,
35 nhiều client, 400 phân trang, 84 UI, 13 luồng hai client thật và cấu hình.
Kiểm tra thêm 500 cách bố trí: hai người và hai báu vật không trùng nhau,
không nằm trên cửa/tường, đều có đường đi; cửa luôn ở A7. Engine nhặt báu vật
theo tọa độ của ván hiện tại, kể cả chạy nhanh đi qua hai báu vật.
Hai client nhận cùng tọa độ bệ qua STATE; cảnh sát vẫn không nhận vị trí
đối thủ, trạng thái báu vật đã mất hoặc việc trộm đang mang đồ.
Đã dựng và xem ảnh Swing trong `build/layout-preview/` để kiểm tra tường gạch.


## Kiểm tra sau khi tổ chức lại kiến trúc — 01/11/2026

Đã chạy đầy đủ `test.bat` trên Windows với JDK 21, biên dịch `--release 17`:
`ALL TESTS PASSED`. Các nhóm đạt: Store 43, engine 5.786, TCP 42,
nhiều client 35, phân trang 400, UI 82 và luồng hai client thật 13 assertions.
Kiểm thử cấu hình mặc định/ghi đè IP và cổng cũng đạt.

`ClientFlowTest` đi qua các callback màn hình và server TCP thật: đăng ký,
đăng nhập, thách đấu, chấp nhận, dùng phím đứng yên, khóa hành động,
đối thủ đóng game và đăng xuất. Kiểm tra người còn lại được trả về sảnh,
kết quả thắng được lưu, vị trí đối thủ vẫn bí mật trong trận.

Đã kiểm tra đóng gói client bằng `tools/package-client.ps1`, biên dịch
`MigrationAudit` và `DemoPreview`, xác nhận package khớp thư mục, main class
của cả hai JAR đúng và không đóng kèm các lớp entry point cũ.
Các kiểm thử tiếp tục dùng dữ liệu trong `work/`, không dùng `data/` thật.

## Kết quả trước khi tổ chức lại

Ngày kiểm tra: 26/10/2026. Môi trường: Windows, OpenJDK Temurin 17.0.19, biên dịch `--release 17`.

## Kết quả cuối cùng

Đã chạy trọn `test.bat` sau các thay đổi và nhận `ALL TESTS PASSED`:

| Nhóm | Kết quả |
|---|---:|
| StoreTest — SQLite, nhập dữ liệu cũ, transaction | 43 assertions đạt |
| EngineTest — luật chơi và bản đồ | 5.786 assertions đạt |
| NetworkTest — TCP, trận đấu, thử lưu lại | 42 assertions đạt |
| PaginationTest — phân trang qua TCP thật | 400 assertions đạt |
| UiTest — giao diện và chuyển trang | 82 assertions đạt |
| Tổng | **6.353 assertions đạt** |

Test mạng cần quyền kết nối localhost. Sandbox mặc định chặn kết nối, vì vậy các lần chạy đầy đủ dùng quyền cho phép TCP localhost. Không có dữ liệu test nào được ghi vào thư mục data thật.

## Hai lỗi đã sửa

1. HISTORY quá dài: server truy vấn và gửi 10 trận/trang, client có nút Trang trước/Trang sau. Bảng xếp hạng cũng phân trang. Kiểm tra đủ 125 trận qua 13 trang, không trùng/thiếu dù thời gian kết thúc bằng nhau; tài khoản không thấy trận của người khác. Mỗi gói đi qua bộ đọc Wire thật và không vượt 16.384 ký tự. Dữ liệu Unicode rất dài chỉ rút gọn khi hiển thị, bản đầy đủ vẫn được lưu. Kiểm tra thêm lịch sử rỗng, trang vượt giới hạn, tham số không hợp lệ và heartbeat sau khi xem lịch sử.
2. Lưu kết quả thất bại: toàn bộ trận, người tham gia và thống kê ghi trong một transaction. Kiểm tra lỗi khi cập nhật người thứ hai và lỗi tại COMMIT bằng foreign key deferred; cả database lẫn đối tượng người chơi online không có thay đổi một phần. Retry cùng mã trận lưu đúng một lần. Server tự thử lại kết quả chờ mỗi 5 giây và vẫn thử sau khi phòng đã đóng; có test gây lỗi tạm thời, khôi phục ghi rồi xác nhận kết quả được lưu duy nhất.

Trong log NetworkTest có stack trace `injected result write failure`. Đây là lỗi SQLite được test chủ động tạo bằng trigger để xác nhận rollback và cơ chế thử lại; test sau đó bỏ trigger, lưu lại thành công và kết thúc PASS.

## Giữ dữ liệu cũ

Fixture `tests/fixtures/nightchase-v1.dat` được tạo bằng Store của bản JAR trước SQLite, gồm ba tài khoản và hai trận thắng/hòa. Test xác nhận mật khẩu cũ còn dùng được, giữ điểm/thống kê, mã trận, thời gian, Unicode, tạo bản sao lưu chính xác và không nhập trùng sau khi khởi động lại. Test dữ liệu hỏng và quan hệ không hợp lệ xác nhận không nhập dở dang; có thể thử lại sau khi khôi phục dữ liệu đúng. Database thuộc phiên bản schema mới hơn bị từ chối để tránh hạ cấp.

Đã nhập và đối chiếu dữ liệu thật bằng `tools/MigrationAudit.java`:

- **2 tài khoản và 3 trận** được chuyển vào `data/museumheist.db`.
- Tất cả salt/hash mật khẩu, điểm, số trận/thắng/thua và mọi trường lịch sử khớp dữ liệu cũ.
- `PRAGMA integrity_check` trả về `ok`; `foreign_key_check` không có lỗi.
- `data/nightchase.dat` giữ nguyên từng byte.
- Bản sao: `data/legacy-backups/nightchase-250d8c2e7fe348f1addcc4c76a2ea9e4288f5fb1f8bc94a6352b95178e173abd.dat`.
- SHA-256 của bản gốc và bản sao: `250d8c2e7fe348f1addcc4c76a2ea9e4288f5fb1f8bc94a6352b95178e173abd`.

Chương trình audit so sánh thông tin xác thực bằng byte, không cần biết hoặc in mật khẩu thật. Một bản sao dữ liệu thật đã được chuyển thử và đối chiếu thành công trước khi nhập vào data thật. Không có chủ phòng Java chạy tại thời điểm chuyển đổi.

## Đóng gói và vận hành

`MuseumHeist.jar` đã được build lại. SQLite JDBC 3.53.4.0 đi kèm trong `lib`; manifest của game tự nạp thư viện này. Các test và công cụ migration đã dùng manifest này để mở SQLite, không cần cấu hình classpath riêng cho JDBC. Không cần cài MySQL/MongoDB hoặc tải thư viện lúc chạy. Giữ cả thư mục lib khi chép game sang máy khác. Schema SQL nằm trong JAR tại `db/schema.sql`, nguồn tại `res/db/schema.sql`.

SQLite bật khóa ngoại, synchronous=FULL và busy_timeout=5000. Có khóa chính/unique, ràng buộc dữ liệu và index. PBKDF2-HMAC-SHA256, salt 16 byte, 120.000 vòng và hash 256 bit giữ nguyên để tương thích tài khoản cũ.

## Chạy lại

Mở `test.bat` bằng môi trường có JDK 17+ và cho phép TCP localhost. Test tạo thư mục riêng trong work. Khi cần kiểm tra toàn vẹn dữ liệu hiện tại mà không đối chiếu với bản .dat cũ, chạy:

```powershell
java -cp MuseumHeist.jar nightchase.server.Store data
```

Lệnh này cũng tự nhập .dat nếu thư mục chưa được chuyển đổi. Đóng chủ phòng trước khi chạy công cụ quản trị hoặc sao lưu.

## Giới hạn

Chưa kiểm tra hai máy vật lý qua Radmin hoặc nghe âm thanh bằng loa/tai nghe thật. SQLite là database cục bộ do một server sở hữu; không chia sẻ trực tiếp file qua VPN. Khi lỗi ổ đĩa kéo dài, kết quả chờ thử lại nằm trong bộ nhớ: giữ chủ phòng mở đến lúc thông báo lưu thành công, vì tắt tiến trình sẽ mất hàng đợi chưa lưu. Các kết quả đã commit được lưu trong database.
