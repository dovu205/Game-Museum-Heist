# TCP protocol phiên bản 2

Mỗi gói là một dòng UTF-8: `TYPE<TAB>base64(field1)<TAB>...`. Base64 bảo toàn Unicode, tab và xuống dòng trong trường. Giới hạn 16.384 ký tự/dòng và 32 trường tính cả TYPE. Không dùng Java deserialization trên mạng. Các tọa độ là chỉ số hàng trước, `row*7+column`, từ 0 đến 48. Danh sách tọa độ cách nhau bằng dấu phẩy, rỗng là không có.

Phiên bản 2 thay đổi ACTION và mở rộng STATE/UPDATE; cả hai máy phải dùng cùng phiên bản. Gói ACTION cũ có thiếu trường bị từ chối, không được suy diễn thành hành động khác.

## Client → server

| Gói | Trường sau TYPE |
|---|---|
| REGISTER, LOGIN | tên, mật khẩu |
| PING | không có |
| LIST, LOGOUT, EXIT, AGAIN | không có |
| INVITE, ACCEPT, REJECT | tên đối thủ |
| RANKING, HISTORY | số trang từ 0, có thể bỏ qua để lấy trang 0 |
| ACTION | matchId, round, turn, kind, direction, secondDirection |

kind là MOVE/DASH/TRAP/SLIP. direction và secondDirection là UP/RIGHT/DOWN/LEFT/STAY. Hành động thông thường gửi secondDirection=STAY; SLIP gửi hai hướng vuông góc. Đứng yên là MOVE/STAY/STAY. Server kiểm tra vai, tài nguyên, bẫy giữ, hướng, vật cản, vùng cấm đặt bẫy, định danh và deadline. Mỗi người chỉ khóa một hành động hợp lệ mỗi nhịp.

## STATE (bắt đầu nhịp)

Chỉ số tính cả TYPE ở vị trí 0:

| Index | Nội dung |
|---|---|
| 1–3 | matchId, round, turn |
| 4–7 | role, opponentName, myScore, opponentScore |
| 8–11 | myPosition, primaryExit=42, relicMask, carrying |
| 12–14 | trapsLeft, slipsLeft, clues |
| 15–18 | deadline, serverNow, note, pedestalCells |
| 19–23 | visibleEnemyPosition, visibleTrapCells, heldTurns, intelligence, exitCells=42,6 |

## UPDATE (sau xử lý)

| Index | Nội dung |
|---|---|
| 1–3 | matchId, round, resolvedTurn |
| 4–8 | myActualPath, myPosition, clues, relicMask, carrying |
| 9–13 | trapsLeft, slipsLeft, privateNote, intelligence, ending |
| 14–17 | visibleEnemyPosition, enemyPathIfEnded, primaryExit=42, serverNow |
| 18–20 | visibleTrapCells, heldTurns, exitCells=42,6 |

ending là NONE/CAPTURE/ESCAPE/TIMEOUT. visibleEnemyPosition=-1 nếu bị ẩn. Khi ván kết thúc mới gửi đường cuối của đối thủ; giữa ván chỉ gửi tọa độ hiện tại nếu đủ tầm nhìn hoặc cảnh sát được bẫy phát hiện. Bẫy chưa mắc không bao giờ gửi cho trộm. Với cảnh sát relicMask/carrying/slipsLeft=-1; với trộm trapsLeft=-1, clues/intelligence rỗng. PedestalCells là tọa độ địa hình công khai, không bao gồm trạng thái thu thập.

clues có dạng `cell,NOISE|TRACE,remainingTurns;...`. Vòng đời tính cả nhịp phát sinh. Tình báo chỉ cung cấp khu vực chụp tại nhịp 8/16/24, không phải vị trí cập nhật liên tục.

## Các gói phản hồi khác

LOGIN_OK, REGISTERED, LOBBY, INVITED, INVITE_CLOSED, LOCKED, ACTION_REJECTED, ROUND_END, FINISHED, BACK, LOGGED_OUT, RANKING, HISTORY, NOTICE, ERROR và PONG giữ các luồng sảnh/tài khoản/phân trang. ACTION_REJECTED mở khóa để chọn lại nếu nhịp còn hạn. LOCKED không tiết lộ hành động đối thủ. ROUND_END gồm kết quả, ván, hai điểm, lý do, tọa độ hai nhân vật và cửa chính. FINISHED gồm WIN/LOSE/DRAW, hai điểm, lý do. RANKING/HISTORY gồm nội dung bảng, page, pages, total.
