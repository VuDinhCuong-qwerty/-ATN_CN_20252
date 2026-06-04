/**
 * select.js — Select Method Page
 */

// Subtle floating animation on the right panel image
(function initRightPanelFloat() {
    const rightImg = document.querySelector('.right-panel img');
    if (!rightImg) return;

    rightImg.style.transition = 'transform 3s ease-in-out';

    let direction = 1;
    setInterval(() => {
        rightImg.style.transform = `scale(1.03) translateY(${direction * 6}px)`;
        direction *= -1;
    }, 3000);
})();
