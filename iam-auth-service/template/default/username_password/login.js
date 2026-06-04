/**
 * login.js — Fintech BKHN Login Page
 */

// Scroll & highlight the available methods section when "Cách khác" is clicked
function showMethods() {
    const methodsSection = document.querySelector('.methods-section');
    if (!methodsSection) return;

    methodsSection.scrollIntoView({ behavior: 'smooth', block: 'center' });

    // Highlight flash effect
    methodsSection.style.transition = 'background 0.3s, border-radius 0.3s';
    methodsSection.style.background = 'rgba(22, 179, 111, 0.08)';
    methodsSection.style.borderRadius = '12px';
    setTimeout(() => {
        methodsSection.style.background = '';
        methodsSection.style.borderRadius = '';
    }, 800);
}

// Highlight the selected method button
function selectMethod(btn) {
    document.querySelectorAll('.method-btn').forEach(b => {
        b.style.background = '';
        b.style.boxShadow = '';
    });
    btn.style.background = 'rgba(22, 179, 111, 0.18)';
    btn.style.boxShadow = '0 2px 10px rgba(22, 179, 111, 0.25)';
}

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