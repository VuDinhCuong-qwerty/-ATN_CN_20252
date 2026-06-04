// 400 Bad Request - page script

document.addEventListener('DOMContentLoaded', function () {

    // Tự động ẩn trang lỗi và redirect về trang trước sau N giây (tuỳ chỉnh)
    var REDIRECT_DELAY = 0; // đặt > 0 (ms) nếu muốn auto redirect, ví dụ 5000 = 5 giây

    if (REDIRECT_DELAY > 0) {
        setTimeout(function () {
            history.back();
        }, REDIRECT_DELAY);
    }

});