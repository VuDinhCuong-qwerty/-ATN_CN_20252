/**
 * otp.js — Fintech BKHN OTP Page
 */

// Đếm ngược 60s, sau đó bật nút "Gửi lại"
(function startCountdown() {
    var seconds = 60;
    var countdownEl = document.getElementById('countdown');
    var countdownWrap = document.getElementById('countdown-wrap');
    var resendBtn = document.getElementById('resendBtn');

    if (!countdownEl || !resendBtn) return;

    var timer = setInterval(function () {
        seconds--;
        if (countdownEl) countdownEl.textContent = seconds;

        if (seconds <= 0) {
            clearInterval(timer);
            // Ẩn dòng đếm ngược, bật nút
            if (countdownWrap) countdownWrap.textContent = '';
            resendBtn.disabled = false;
        }
    }, 1000);
})();

// Gửi lại OTP — reset countdown
function resendOtp() {
    var resendBtn = document.getElementById('resendBtn');
    var countdownEl = document.getElementById('countdown');
    var countdownWrap = document.getElementById('countdown-wrap');

    if (!resendBtn) return;

    // Reset UI
    resendBtn.disabled = true;
    var seconds = 60;
    if (countdownWrap) countdownWrap.innerHTML = 'Gửi lại sau <strong id="countdown">' + seconds + '</strong>s';
    countdownEl = document.getElementById('countdown');

    // TODO: gọi API gửi lại OTP ở đây nếu cần

    var timer = setInterval(function () {
        seconds--;
        if (countdownEl) countdownEl.textContent = seconds;
        if (seconds <= 0) {
            clearInterval(timer);
            if (countdownWrap) countdownWrap.textContent = '';
            resendBtn.disabled = false;
        }
    }, 1000);
}

// Tô đỏ input khi có lỗi
(function highlightErrorInputs() {
    var errorBox = document.querySelector('.error-box');
    if (!errorBox) return;
    document.querySelectorAll('.input-field').forEach(function (input) {
        input.classList.add('is-error');
        input.addEventListener('input', function () {
            input.classList.remove('is-error');
        }, { once: true });
    });
})();

// Chỉ cho nhập số vào ô OTP
(function restrictOtpInput() {
    var otpInput = document.getElementById('otp');
    if (!otpInput) return;
    otpInput.addEventListener('input', function () {
        this.value = this.value.replace(/[^0-9]/g, '');
    });
})();

// Floating animation ảnh bên phải
(function initRightPanelFloat() {
    var rightImg = document.querySelector('.right-panel img');
    if (!rightImg) return;
    rightImg.style.transition = 'transform 3s ease-in-out';
    var direction = 1;
    setInterval(function () {
        rightImg.style.transform = 'scale(1.03) translateY(' + (direction * 6) + 'px)';
        direction *= -1;
    }, 3000);
})();