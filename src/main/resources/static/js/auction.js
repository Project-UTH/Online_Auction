// Client-side example (e.g., in /src/main/resources/static/js/auction.js)
document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('jwtToken'); // Lấy token từ localStorage sau khi login
    const auctionId = document.getElementById('auctionId').value; // Giả sử có input hidden với id="auctionId" trong template Thymeleaf
    const currentUsername = localStorage.getItem('username'); // Lấy username từ localStorage (lưu sau login)

    if (!token || !auctionId) {
        console.error('Token hoặc Auction ID không tồn tại. Vui lòng đăng nhập lại.');
        return;
    }

    const socket = io('http://localhost:9090', {
        query: {
            token: token,
            auctionId: auctionId
        }
    });

    socket.on('connect', () => {
        console.log('Kết nối Socket.IO thành công');
    });

    socket.on('bidUpdate', (data) => {
        console.log(`Cập nhật giá cho sản phẩm ${data.productId}: $${data.amount} từ ${data.username}`);
        // Cập nhật UI: Ví dụ, cập nhật giá hiển thị trên trang
        const priceElement = document.getElementById(`currentPrice_${data.productId}`);
        if (priceElement) {
            priceElement.textContent = `$${data.amount}`;
        }
        // Thêm thông báo hoặc cập nhật danh sách bid
    });

    socket.on('outbidNotification', (data) => {
        if (data.targetUsername === currentUsername) {
            console.log(data.message);
            // Hiển thị alert hoặc cập nhật UI cho thông báo bị vượt giá
            alert(data.message);
        }
    });

    // Gửi bid khi người dùng click button (ví dụ)
    const bidButtons = document.querySelectorAll('.bid-button');
    bidButtons.forEach(button => {
        button.addEventListener('click', (e) => {
            const productId = e.target.dataset.productId;
            const amount = parseFloat(document.getElementById(`bidAmount_${productId}`).value);
            if (!isNaN(amount)) {
                socket.emit('placeBid', { productId: parseInt(productId), amount: amount });
            } else {
                alert('Vui lòng nhập số tiền hợp lệ.');
            }
        });
    });
});