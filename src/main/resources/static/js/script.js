// Countdown Timer
function updateCountdown() {
    // Set target date (25/08/2025 16:00)
    const targetDate = new Date('2025-08-25T16:00:00+07:00').getTime();
    const now = new Date().getTime();
    const distance = targetDate - now;
    if (distance > 0) {
        const days = Math.floor(distance / (1000 * 60 * 60 * 24));
        const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);
        document.getElementById('days').textContent = days.toString().padStart(2, '0');
        document.getElementById('hours').textContent = hours.toString().padStart(2, '0');
        document.getElementById('minutes').textContent = minutes.toString().padStart(2, '0');
        document.getElementById('seconds').textContent = seconds.toString().padStart(2, '0');
    } else {
        // Auction has started
        document.getElementById('days').textContent = '00';
        document.getElementById('hours').textContent = '00';
        document.getElementById('minutes').textContent = '00';
        document.getElementById('seconds').textContent = '00';
        document.querySelector('.countdown-title').textContent = 'Phiên đấu giá đang diễn ra!';
        document.querySelector('.next-auction').innerHTML = '<span class="next-auction-highlight">Tham gia ngay!</span>';
    }
}
// Update countdown every second
updateCountdown();
setInterval(updateCountdown, 1000);
// Add some interactive effects
document.querySelector('.auction-btn').addEventListener('click', function() {
    this.style.transform = 'scale(0.95)';
    setTimeout(() => {
        this.style.transform = 'translateY(-1px)';
    }, 150);
});