# Luật chơi Cảnh sát và Trộm — phiên bản 2

## 1. Giới thiệu

Trò chơi đối kháng trực tuyến hai người theo mô hình Client–Server. Trộm lấy báu vật rồi thoát; cảnh sát tìm và bắt trộm. Hai người chọn hành động bí mật, server kiểm tra và xử lý đồng thời. Game không có chat.

## 2. Trận đấu

- Mỗi trận gồm 2 ván, đổi vai sau ván đầu. Vai đầu tiên được chọn ngẫu nhiên.
- Mỗi ván tối đa 32 nhịp, mỗi nhịp có 8 giây chọn hành động.
- Server xử lý ngay khi nhận đủ hai hành động hợp lệ. Hết giờ, người chưa gửi hành động hợp lệ tự đứng yên.
- Thắng một ván được 1 điểm. Kết quả 2–0 hoặc 0–2 xác định người thắng; 1–1 là hòa, không có hiệp phụ.

## 3. Bản đồ

Bản đồ 7×7, cột A–G, hàng 1–7. Đường đi màu xanh xám; tường là gạch nâu. Không thể đi xuyên tường hoặc ra ngoài bản đồ. Các đường đi đều kết nối.

Đầu mỗi ván, hai nhân vật và hai báu vật được chọn tại bốn ô đường đi khác nhau, không trùng cửa thoát. Bệ trưng bày đánh dấu các vị trí báu vật. Trộm chỉ cần lấy một báu vật.

Hai cửa cố định tại **A7 và G1**, ở hai góc đối diện; cả hai người đều thấy. Trộm có thể thoát ở bất kỳ cửa nào sau khi có báu vật. Đến cửa khi chưa có báu vật không kết thúc ván.

## 4. Di chuyển

- **Đi bộ:** một ô lên, xuống, trái hoặc phải. Hướng bị chặn phải chọn lại.
- **Chạy nhanh:** cả hai vai đều dùng được, tối đa hai ô thẳng hàng, không giới hạn số lần. Gặp tường dừng tại ô hợp lệ cuối cùng. Bước đầu bị chặn thì chọn lại. Chỉ trộm chạy mới tạo tiếng động tại ô xuất phát.
- **Đứng yên:** giữ nguyên vị trí. Có thể chủ động chọn hoặc do hết giờ.

## 5. Cảnh sát: Đặt bẫy

Cảnh sát thắng khi bắt trộm hoặc trộm chưa thoát sau nhịp 32.

Đặt bẫy tối đa **2 lần mỗi ván**, thay cho di chuyển:

1. Chọn một hướng để đặt tại ô đường đi kề. Không đặt vào tường, ngoài bản đồ, ô đã có bẫy, cửa thoát hoặc ô sát bất kỳ cửa nào, kể cả đường chéo.
2. Bẫy cố định, tồn tại hết ván và bắt đầu hoạt động từ nhịp tiếp theo.
3. Khi trộm **đi vào** bẫy đang hoạt động, trộm dừng ngay tại ô bẫy; cảnh sát biết tọa độ trộm.
4. Nhịp tiếp theo trộm chỉ được đứng yên. Sau nhịp bị giữ, trộm di chuyển lại bình thường.
5. Bẫy không mất sau kích hoạt. Đứng yên trên bẫy không kích hoạt liên tục; đi ra rồi vào lại có thể mắc lại.

Cảnh sát thấy mọi bẫy đã đặt. Trộm chỉ thấy bẫy từng mắc phải. Tọa độ được bẫy phát hiện hiển thị cho cảnh sát khi kích hoạt và trong nhịp giữ; sau đó áp dụng lại tầm nhìn thông thường.

## 6. Trộm: Luồn lách

Trộm thắng khi có ít nhất một báu vật và tới A7 hoặc G1 trước khi bị bắt.

Luồn lách tối đa **3 lần mỗi ván**:

- Chọn lần lượt hai hướng vuông góc, di chuyển tối đa hai ô và rẽ 90 độ sau bước đầu.
- Không tạo tiếng động. Không đi xuyên tường hoặc ra ngoài bản đồ.
- Bước đầu bị chặn thì chọn lại. Nếu chỉ bước thứ hai bị chặn, dừng sau bước đầu và vẫn tiêu hao một lượt kỹ năng.
- Vẫn bị bắt hoặc mắc bẫy trên từng bước.
- Ví dụ hợp lệ trên bản đồ hiện tại: B7 → B6 → A6 (lên rồi trái).

Trên bản đồ này D3 là tường nên ví dụ C3 → D3 → D4 không hợp lệ. Quy tắc kỹ năng không cho phép đi xuyên tường.

Báu vật được nhặt tự động khi trộm đi vào hoặc đi qua ô báu vật, kể cả bước đầu của chạy nhanh/luồn lách. Không cần nút nhặt riêng.

## 7. Thông tin và dấu vết

- Luôn thấy nhân vật của mình. Sau xử lý, nếu hai nhân vật cùng hàng/cột, cách tối đa hai ô và không có tường chắn, cả hai thấy nhau. Ngoài phạm vi, đối thủ bị ẩn trừ thông tin từ bẫy. Không gửi đường di chuyển bí mật của đối thủ.
- **Tiếng động:** vòng cam tại ô trộm bắt đầu chạy nhanh, tồn tại hai nhịp tính cả nhịp tạo; chỉ cảnh sát thấy.
- **Dấu chân:** trộm đi bộ ở nhịp chia hết cho 3 để lại dấu tại ô xuất phát, tồn tại ba nhịp tính cả nhịp tạo. Đứng yên không tạo dấu.
- **Chuông báu vật:** cảnh sát được báo có báu vật bị lấy, không nhận vị trí trộm, số báu vật đang mang hoặc trạng thái báu vật còn lại. Bệ trưng bày trên giao diện cảnh sát không đổi theo việc thu thập.
- **Tình báo:** sau nhịp 8, 16, 24 nếu ván còn diễn ra, cảnh sát nhận khu vực hiện tại của trộm: Bắc (hàng 1–2), Trung tâm (3–5), Nam (6–7). Đây là thông tin tại thời điểm đó, không theo dõi liên tục và không tiết lộ tọa độ.

## 8. Xử lý đồng thời

Server xử lý lần lượt các bước con. Nhân vật chỉ đi một ô hoặc đứng yên giữ nguyên vị trí trong bước con còn lại.

Tại mỗi bước:

1. Cập nhật vị trí hai nhân vật cùng lúc.
2. Kiểm tra bắt giữ: cùng ô hoặc đổi chỗ trong cùng bước. Bắt kết thúc ván ngay.
3. Kiểm tra bẫy nếu trộm đi vào ô bẫy đang hoạt động; dừng phần đường còn lại của trộm. Cảnh sát vẫn có thể đi bước còn lại và bắt trộm đang mắc bẫy.
4. Tự động nhặt báu vật khi trộm đi qua.
5. Kiểm tra thoát nếu trộm có báu vật và ở cửa. Thoát thành công dừng mọi bước còn lại.

Bắt giữ luôn ưu tiên hơn thoát trong cùng bước. Không thể chạy xuyên cảnh sát. Ván chưa kết thúc sau bước cuối nhịp 32 thì cảnh sát thắng.

## 9. Kết thúc, rời trận và chơi lại

Sau ván một, đổi vai và tạo lại vị trí xuất phát/báu vật; kỹ năng và bẫy được đặt lại. Sau hai ván, tổng hợp điểm thắng/thua/hòa. Khi kết thúc ván, công khai vị trí hai nhân vật.

Rời trận hoặc mất kết nối khi trận đang diễn ra được tính đầu hàng, đối thủ thắng trận. Không cộng điểm cho ván chưa chơi xong. Tài khoản, điểm và lịch sử lưu trên SQLite của server.

Sau khi kết thúc, trận mới chỉ bắt đầu khi cả hai chọn **Chơi lại**.

## 10. Điều khiển

| Thao tác | Chức năng |
|---|---|
| ↑ ↓ ← → | Chọn hướng |
| Enter | Khóa và gửi hành động |
| Space hoặc nút • | Đứng yên |
| Nhấp ô kề | Chọn hướng, chưa tự gửi |
| Chạy nhanh | Hai ô cùng hướng |
| Đặt bẫy | Cảnh sát chọn hướng ô kề |
| Luồn lách | Trộm chọn hai hướng vuông góc |

Khi luồn lách, lần chọn hướng đầu xác định bước một; lần chọn hướng vuông góc kế tiếp xác định bước hai. Chọn tiếp sau khi đủ hai hướng sẽ bắt đầu chọn lại. Nhấp bản đồ cho bước hai theo ô sau bước một.

Trước khi khóa, giao diện vẽ đường dự kiến, gồm hai đoạn vuông góc với luồn lách. Bẫy được đánh dấu tại ô dự kiến. Sau khi khóa không được đổi hành động. Hành động bị server từ chối có thể chọn lại trong thời gian còn lại. Sang nhịp mới, tự mở khóa và chọn lại từ Đi bộ/Đứng yên.

Nhân vật xanh là cảnh sát, nhân vật đeo mặt nạ là trộm; chữ BẠN đánh dấu bản thân. Hình thoi vàng là báu vật, cửa xanh là lối thoát, vòng cam là tiếng động, hai vết nhỏ là dấu chân, vòng có dấu chéo đỏ là bẫy. Nhật ký chỉ hiển thị thông tin người chơi có quyền biết.

## 11. Mục tiêu thiết kế

Hai cửa thoát hạn chế việc canh một cửa. Cảnh sát kiểm soát lối đi bằng bẫy và kết hợp dấu vết, tầm nhìn, tình báo; trộm dùng luồn lách để rẽ đột ngột và chọn lối thoát. Hai ván đổi vai giúp cân bằng trải nghiệm. Các thông số 32 nhịp/8 giây có thể được điều chỉnh sau chơi thử.
