> Tài liệu lịch sử của bản trước. Bản 2 xem [kiến trúc](architecture.md), [luật chơi](rules.md), [kiểm thử](verification.md).

# Báo cáo kiểm tra game — 26/10/2026

**Cập nhật sau báo cáo:** Hai lỗi bên dưới đã được sửa trong bản SQLite. Đã chuyển dữ liệu thật (2 tài khoản, 3 trận), giữ nguyên .dat và tạo bản sao lưu. Toàn bộ test mới đạt; xem `KIEM_THU.md` và `../README.md`. Phần còn lại của báo cáo này ghi nhận cách tái hiện trên phiên bản trước khi sửa, bao gồm `work/ReviewProbe.java` chỉ dành cho bản .dat cũ.

Môi trường: Windows, OpenJDK Temurin 17.0.19. Kiểm tra mã nguồn, build và chạy các bộ test. Dữ liệu bổ sung nằm riêng trong `work/`, không dùng tài khoản trong `data/`.

## Kết quả test

- Build thành công với `--release 17`.
- EngineTest: PASS, 5.786 assertions.
- NetworkTest: PASS, 38 assertions TCP localhost.
- UiTest: PASS, 70 assertions.
- Lần đầu chạy trong sandbox, test mạng bị từ chối quyền kết nối localhost. Chạy ngoài sandbox thành công.

## Lỗi đã tái hiện

### 1. Mở lịch sử nhiều trận làm client mất kết nối

Vị trí: `Server.java:83–86`, `Store.java:42`, `Wire.java:21`.

Server ghép tối đa 100 trận vào một thông điệp HISTORY. Sau Base64, thông điệp có thể vượt giới hạn 16.384 ký tự mà Wire.read cho phép. ClientService bắt lỗi đọc và đóng kết nối, đưa người chơi về đăng nhập; nếu người chơi đang trong trận thì mất kết nối còn có thể bị xử đầu hàng.

Đã tạo 100 kết quả hòa với UUID và lý do thông thường “Bảo tàng · Hoàn tất hai ván đổi vai”, đăng nhập bằng TCP thật và gửi HISTORY. Wire.read báo `Message too large`.

Hướng sửa: phân trang lịch sử và giới hạn kích thước thông điệp sau mã hóa; kiểm tra tương tự cho RANKING khi số tài khoản tăng.

### 2. Lưu kết quả thất bại để lại trạng thái đã cộng điểm trong bộ nhớ

Vị trí: `Store.java:43–48`.

Store.finish cộng điểm, tăng thống kê và thêm trận trước khi save. Nếu save thất bại, các thay đổi trong bộ nhớ vẫn còn. Thử lưu lại cùng mã trận bị nhánh kiểm tra trùng bỏ qua, nên không ghi lại kết quả đó. Nếu tắt server trước một lần lưu khác thành công, kết quả sẽ mất.

Đã gây lỗi ghi bằng cách tạo một thư mục tên `nightchase.tmp` trong thư mục dữ liệu test. Sau IOException, tài khoản trong bộ nhớ có 1 điểm và 1 trận; bỏ vật cản rồi gọi finish lại cùng mã trận, mở Store từ file vẫn có 0 điểm và 0 trận.

Hướng sửa: rollback toàn bộ thay đổi khi lưu thất bại hoặc chỉ thay trạng thái đang dùng sau khi snapshot mới đã được ghi thành công; bảo đảm thao tác retry có thể lưu lại.

## Tái chạy kiểm tra bổ sung

Mã tái hiện: `work/ReviewProbe.java`.

```powershell
javac -encoding UTF-8 --release 17 -cp 'MuseumHeist.jar;build\tests' -d build\tests work\ReviewProbe.java
java '-Dfile.encoding=UTF-8' -cp 'MuseumHeist.jar;build\tests' nightchase.ReviewProbe
```

Đầu ra xác nhận:

```text
SAVE_FAILURE: in-memory points=1, history=1
SAVE_RETRY: disk points=0, history=0
HISTORY_100: confirmed client read failure: Message too large
REVIEW PROBES COMPLETE (2 bugs reproduced)
```

Chưa sửa mã nguồn ứng dụng. Build đã tạo lại MuseumHeist.jar. Chưa kiểm tra hai máy vật lý qua Radmin, âm thanh trên loa thật hoặc chơi tay đầy đủ.
