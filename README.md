# MUSEUM HEIST — Truy bắt trong bảo tàng

Game Java Swing / Java2D, giao tiếp TCP Socket. Một máy vừa chạy server vừa chơi, máy còn lại kết nối qua Radmin VPN. Bản này triển khai ý tưởng bản đồ 7×7 và thông tin ẩn; thay cho cơ chế chọn ba hướng, câu đố và hòm tiếp tế của bản trước.

## Mở game

Cần Java 17 trở lên. Không cần IDE, Maven, Gradle hoặc cài dịch vụ database để chơi; file JAR đã được biên dịch. Giữ thư mục **lib** cạnh MuseumHeist.jar vì chứa SQLite JDBC đi kèm.

1. Giải nén toàn bộ bộ game trên hai máy. Cả hai phải dùng cùng bản Museum Heist.
2. Giữ Radmin VPN mở; hai máy tham gia cùng mạng Radmin.
3. **Máy 1:** mở **May1-ChuPhong.bat**. Server tự chạy nền ở TCP **5555**, rồi hiện màn hình đăng nhập. Không có nút mở server trong giao diện.
4. **Máy 2:** mở **May2-ThamGia.bat**. Trên màn hình đăng nhập chọn **Kết nối phòng chơi…**, nhập **IPv4 Radmin của máy 1** và cổng **5555**, nhấn OK.
5. Đăng ký hai tài khoản khác nhau (hoặc dùng tài khoản cũ đã sao chép dữ liệu), rồi đăng nhập. Đăng nhập và đăng ký là hai màn hình riêng; menu chỉ hiện sau đăng nhập.
6. Chọn đối thủ đang Rỗi trong sảnh → Thách đấu → người kia Chấp nhận.

ChoiGame.bat là lối mở client tham gia, tương đương May2-ThamGia.bat. Để thử một máy: mở May1-ChuPhong.bat một lần và May2-ThamGia.bat một lần; client tham gia dùng IP 127.0.0.1.

Giữ game máy 1 mở suốt trận. Đóng game máy 1 sẽ dừng server. Chỉ mở một bản chủ phòng trên cổng 5555.

## Giữ tài khoản cũ

Đóng game cũ và sao lưu thư mục data. Chép thư mục **data** từ bộ Night Chase cũ vào cạnh **MuseumHeist.jar** trên máy 1. Không ghi đè nếu nơi nhận đã có dữ liệu cần giữ.

Khi chủ phòng mở lần đầu, game tự nhập data/nightchase.dat sang **data/museumheist.db** bằng một transaction. Giữ nguyên tên, salt/hash mật khẩu, điểm, thống kê và lịch sử cũ; không cần đặt lại mật khẩu. Điểm của cả hai phiên bản được cộng chung nếu giữ dữ liệu cũ.

File .dat gốc được giữ nguyên, đồng thời tạo bản sao tại **data/legacy-backups/nightchase-<SHA-256>.dat** trước khi nhập. Mở lại game không nhập trùng. Nếu dữ liệu cũ không hợp lệ, game dừng nhập và giữ bản gốc để xử lý, không âm thầm tạo tài khoản trắng.

Nếu nơi nhận đã có museumheist.db, game dùng database đó; không tự gộp thêm một file .dat khác. Muốn chuyển từ bộ cũ, dùng thư mục game mới chỉ chép dữ liệu cũ vào data trước lần mở đầu. Muốn chơi với bảng thành tích mới, dùng thư mục game mới không chép data. Chỉ một server sở hữu thư mục data; không dùng chung file .db qua thư mục mạng/VPN.

Sau khi chuyển đổi, các trận mới chỉ ghi vào SQLite. File .dat là dữ liệu trước chuyển đổi, không chứa tiến trình mới. Sao lưu toàn bộ thư mục data sau khi đóng chủ phòng; không chạy bản JAR cũ để tiếp tục dữ liệu SQLite.

## Cơ sở dữ liệu SQLite

- `users`: tên tài khoản duy nhất, salt, hash PBKDF2 và thống kê.
- `matches`: mã trận duy nhất, thời gian, số ván, người thắng và lý do kết thúc.
- `match_players`: hai người tham gia, thứ tự người chơi và điểm của từng người; có khóa ngoại đến tài khoản và trận.
- `metadata` và `PRAGMA user_version`: trạng thái nhập dữ liệu cũ và phiên bản schema.
- Có index cho xếp hạng và lịch sử; dùng prepared statement, bật foreign_keys và synchronous=FULL.
- Ghi trận và cộng điểm trong cùng transaction. Nếu lỗi, rollback cả trận lẫn thống kê; trạng thái online chỉ đổi sau commit. Server giữ kết quả chờ trong bộ nhớ và thử lưu lại mỗi 5 giây, kể cả khi đã rời phòng. Nếu có thông báo chưa lưu, giữ chủ phòng mở đến khi báo lưu thành công; hàng đợi chưa lưu không tồn tại qua việc tắt tiến trình.
- Lịch sử và bảng xếp hạng hiển thị **10 bản ghi mỗi trang**, có nút Trang trước/Trang sau. Lịch sử qua giao diện truy cập được cả các trận vượt mốc 100; không gửi toàn bộ trong một gói TCP. Chuỗi quá dài trong dữ liệu cũ chỉ được rút gọn khi hiển thị, bản đầy đủ vẫn nằm trong database.

Có thể dùng công cụ SQLite bên ngoài để đọc database khi đã đóng game. Không sửa database lúc chủ phòng đang chạy.

## Luật chơi

Mỗi trận có **hai ván đổi vai**, vai đầu được server chọn ngẫu nhiên. Mỗi ván tối đa **20 nhịp**, mỗi nhịp tối đa **5 giây**. Hai người gửi quyết định bí mật; server cập nhật đồng thời khi nhận đủ hai lựa chọn hoặc hết hạn.

### Trộm

- Lấy một trong hai báu vật tại **B5** hoặc **F3**, rồi tới cửa thoát.
- Một cửa thoát hoạt động được server chọn ngẫu nhiên mỗi ván trong các vị trí A7, G1, A4, G4. **Chỉ trộm biết vị trí**. Cảnh sát không được gửi tọa độ cửa thoát trong ván.
- Chạy nhanh đi tối đa hai ô thẳng hàng, dừng trước tường; gây tiếng động ở ô xuất phát. Không giới hạn số lần chạy nhanh.
- Mồi nhử tạo tiếng động ở một ô kề hợp lệ, không di chuyển. Dùng một lần mỗi ván.
- Báu vật được nhặt tự động khi đi qua ô có báu vật. Đã có báu vật và đi tới cửa thoát sẽ kết thúc ván; chạy nhanh dừng ngay khi thoát thành công.

### Cảnh sát

- Bắt trộm hoặc bảo vệ hết 20 nhịp để thắng ván.
- Quét khu vực kiểm tra vùng 3×3 quanh vị trí của mình, gồm các ô chéo; quét xuyên tường trong vùng đó.
- Quét chỉ báo có hoặc không có trộm, không cung cấp ô chính xác. Dùng tối đa hai lần mỗi ván, thay cho di chuyển.
- Nhận tiếng động và dấu chân, không thấy toàn bộ vị trí đối thủ.
- Tiếng động từ chạy nhanh và từ mồi nhử có cùng hình thức, hiện qua hai nhịp. Dấu chân xuất hiện ở ô xuất phát khi trộm đi bộ vào các nhịp chia hết cho 3, tồn tại qua ba nhịp.
- Các bệ trưng bày trên bản đồ là mốc địa hình. Cảnh sát không thấy bệ nào vừa mất báu vật hoặc trạng thái trộm đang mang đồ; chỉ có thông báo chuông chung khi báu vật bị lấy.

### Bắt, thoát và hết giờ

- Kết thúc một bước con ở cùng ô hoặc đi đổi chỗ qua nhau: cảnh sát bắt trộm.
- Chạy nhanh được kiểm tra từng bước con, không đi xuyên qua cảnh sát.
- Bắt giữ ưu tiên hơn thoát nếu xảy ra ở cùng bước.
- Hết 5 giây không gửi: tự đứng yên trong nhịp đó. Không bị xử thua ngay cả ván.
- Sau nhịp 20, nếu chưa bắt hoặc thoát: cảnh sát thắng.
- Vị trí hai người chỉ được công khai khi ván đã kết thúc.
- Thắng ván +1 điểm. Sau hai ván: 2–0 hoặc 0–2 phân thắng bại; 1–1 là hòa. Không có hiệp phụ.
- Cả hai chọn Chơi lại để bắt đầu trận mới. Rời giữa trận hoặc mất kết nối được xử đầu hàng; đối thủ thắng trận, không tự cộng điểm ván chưa hoàn tất.

## Điều khiển

1. Chọn loại hành động ở bảng bên phải.
2. Chọn hướng bằng nút mũi tên hoặc phím mũi tên.
3. Nhấn **Khóa hành động** hoặc **Enter** để gửi.

- Nút giữa / **Space**: đứng yên.
- Nhấp một ô kề trên bản đồ: chọn hướng, chưa tự gửi.
- Chạy nhanh / mồi nhử cần chọn hướng. Quét tự chọn đứng yên.
- Trước khi gửi, đường nét đứt chỉ là dự kiến của bạn.
- Sau khi khóa, không được đổi. Hướng bị tường chắn hoặc kỹ năng hết lượt bị từ chối; có thể chọn lại nếu nhịp còn thời gian.
- Xanh dương là cảnh sát, nhân vật mặt nạ là trộm; chữ BẠN đánh dấu nhân vật của mình.
- Hình thoi vàng: báu vật. Vòng cam: tiếng động. Hai dấu nhỏ: dấu chân. Ô xanh: vùng quét. Cửa xanh lá: lối thoát của trộm.

Nhật ký bên phải là thông tin riêng của người chơi. Không có chat giữa hai bên hoặc thông tin tự động tiết lộ hành động đối thủ.

## Âm thanh

Nhạc riêng cho đăng nhập/sảnh, truy bắt và các nhịp cuối. Có âm khóa hành động, tiếng quét, lấy đồ, bắt và thoát. Âm thanh WAV được tổng hợp bằng script đi kèm, nhúng trong JAR. Tắt âm hoặc chỉnh âm lượng trong menu sau đăng nhập. Không có thiết bị audio thì game vẫn chạy.

## Kết nối và xử lý lỗi

- Nếu máy 1 báo cổng đang bận: đóng bản game chủ phòng cũ trước khi mở bản mới.
- Nếu máy 2 không kết nối: kiểm tra Radmin cùng mạng, đúng IP của máy 1, Java được Windows Firewall cho phép và inbound TCP 5555 trên máy 1.
- Có thể kiểm tra từ PowerShell máy 2: Test-NetConnection IP_RADMIN_MAY_1 -Port 5555. Thay IP_RADMIN_MAY_1 bằng địa chỉ thực tế.
- Cho phép game trên mạng Radmin đang dùng; không cần tắt toàn bộ firewall hoặc mở cổng router.
- Cần đổi cổng: máy 1 chạy java -Dnightchase.port=5556 -jar MuseumHeist.jar --host; máy 2 đổi cổng trong Kết nối phòng chơi. Khi gõ trong PowerShell, đặt tham số -D trong dấu nháy đơn.
- Khi mạng đứt im lặng, heartbeat / timeout socket phát hiện trong khoảng tối đa 45 giây. Không khôi phục ván cũ khi đăng nhập lại.
- Server lưu dữ liệu ở data/museumheist.db, dùng PBKDF2-HMAC-SHA256 và salt ngẫu nhiên. Không lưu mật khẩu thô. Sao lưu data sau khi tắt server.

## Mã nguồn

src/nightchase/
- Museum.java: luật bản đồ, kiểm tra hành động, bước con, va chạm, báu vật, dấu vết.
- Server.java: TCP, tài khoản online, lời mời, đồng hồ, phòng, dữ liệu riêng từng vai.
- Client.java: Swing, đăng nhập/đăng ký riêng, sảnh, bản đồ Java2D, điều khiển, hiệu ứng và âm thanh.
- ClientService.java: gửi/nhận trên luồng nền và heartbeat.
- Store.java: SQLite JDBC, nhập dữ liệu cũ, tài khoản, PBKDF2, transaction kết quả và truy vấn phân trang.
- Wire.java: dòng UTF-8, Base64 cho từng trường, giới hạn độ dài.
- Audio.java: phát WAV, loop, mute và âm lượng.
- Launcher.java: khởi động server nền trên máy 1 trước khi mở game.

build.bat: biên dịch bằng JDK 17+, đóng gói MuseumHeist.jar.
test.bat: build rồi kiểm thử nhập dữ liệu/transaction SQLite, engine, TCP thật, phân trang và giao diện. Cần cho phép kết nối TCP localhost.
assets/db/schema.sql: schema SQLite phiên bản 1.
lib/: JDBC SQLite đi kèm, không cần tải mạng lúc chơi/build/test.
tools/MigrationAudit.java: đối chiếu toàn bộ tài khoản và trận sau khi nhập dữ liệu cũ.
tools/DemoPreview.java: tạo GIF bằng chính engine và canvas của game.
preview/: ảnh và GIF render từ giao diện thật.

Giao thức mới không tương thích client Night Chase cũ. Cả hai máy phải cập nhật cùng bộ Museum Heist. Các giá trị map, thời hạn và số nhịp mặc định nằm trong Museum.java; nếu thay đổi hằng số cần rà lại các nhãn giao diện liên quan.

Xem KIEM_THU.md để biết các kiểm tra đã chạy và giới hạn thực tế.
