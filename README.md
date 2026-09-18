# Auto Clicker (AutoClick2) 🖱️

Ứng dụng Auto Clicker (Tự động click) tiên tiến dành cho Android được viết bằng ngôn ngữ Java, sử dụng **Dịch vụ Trợ năng (Accessibility Service)** và **Cửa sổ nổi (Floating Window Overlay)**. Ứng dụng cho phép bạn tự động hóa các thao tác chạm trên mọi ứng dụng hoặc trò chơi mà không cần root thiết bị.

---

## ✨ Tính năng chính

- **Không cần Root**: Sử dụng API `dispatchGesture` gốc của Android Accessibility Service để mô phỏng thao tác chạm an toàn và chính xác.
- **Bảng điều khiển nổi**: Thanh công cụ nổi trực quan, tiện lợi có thể kéo đi bất cứ đâu trên màn hình, hỗ trợ:
  - ➕ **Thêm điểm**: Thêm các điểm click mới được đánh số (`1, 2, 3...`).
  - ➖ **Xóa điểm**: Xóa điểm click gần nhất.
  - ⚙ **Cài đặt**: Tỉnh chỉnh thời gian và chế độ ngay trên màn hình nổi.
  - ▶ / ⏹ **Start / Stop**: Bật/tắt auto click tức thì trên mọi ứng dụng/game.
- **Quản lý đa điểm click**:
  - Tạo và quản lý nhiều điểm click cùng lúc.
  - Kéo thả từng điểm đến bất kỳ vị trí mong muốn trên màn hình.
  - Thiết kế vòng tròn mục tiêu với tâm màu đỏ giúp định vị chính xác tuyệt đối.
- **Cài đặt nâng cao**:
  - **Delay giữa các lần click**: Tùy chỉnh theo đơn vị mili-giây (ms) hoặc giây (s).
  - **Thời gian giữ click (Duration)**: Điều chỉnh thời gian giữ mỗi nhịp chạm.
  - **Chế độ click**:
    - *Multi Point*: Click tuần tự vòng lặp (`A → B → C → A → B → C...`).
    - *Single Point*: Click liên tục tại điểm số 1.
    - *Random*: Chọn ngẫu nhiên các điểm trong danh sách.
  - **Số lần lặp (Repeat)**: Lựa chọn 1 lần, 10 lần, 100 lần hoặc Vô hạn (`∞`).
- **Giao diện trang chủ hiện đại**: Giao diện tối (Dark mode) trực quan, hiển thị trạng thái Trợ năng, số lượng điểm click, delay và lặp rõ ràng.

---

## 📱 Yêu cầu hệ thống

- **Phiên bản Android**: Android 7.0 (API 24) trở lên.
- **Quyền yêu cầu**:
  - **Dịch vụ Trợ năng (Accessibility Service)**: Dùng để mô phỏng cử chỉ chạm trên màn hình.
  - **Hiển thị trên ứng dụng khác (SYSTEM_ALERT_WINDOW)**: Dùng để hiển thị các điểm click nổi và bảng điều khiển.

---

## 🚀 Hướng dẫn sử dụng

1. **Bật Trợ năng**:
   - Mở ứng dụng và bấm **Bật Accessibility**.
   - Tìm ứng dụng **Auto Clicker** trong phần Cài đặt Trợ năng của thiết bị và bật **Bật (ON)**.
2. **Thêm điểm Click**:
   - Quay lại ứng dụng và bấm **Add Point** (hoặc dùng nút `+` trên bảng nổi).
   - Kéo các vòng tròn số (`1, 2, 3...`) đến vị trí bạn muốn click trên màn hình.
3. **Cài đặt thông số**:
   - Bấm nút **⚙ Settings** để chỉnh delay, thời gian giữ, chế độ click và số lần lặp.
4. **Chạy Auto Click**:
   - Bấm **▶ START** (trên app hoặc trên bảng điều khiển nổi) để bắt đầu tự động click!

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ**: Java
- **Giao diện**: Android Views (XML & Programmatic Views), Material Design
- **Core APIs**: `AccessibilityService` (`dispatchGesture`), `WindowManager` (Floating Overlays)

---

## 📄 Giấy phép

Dự án mã nguồn mở phục vụ cho mục đích học tập và cá nhân.
