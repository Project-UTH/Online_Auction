document.addEventListener('DOMContentLoaded', () => {
    // Debug: Kiểm tra tất cả thông tin cần thiết
    const token = localStorage.getItem('jwtToken');
    const auctionId = document.getElementById('auctionId')?.value;
    const currentUsername = localStorage.getItem('username');

    console.log('=== DEBUG INFORMATION ===');
    console.log('Token:', token ? 'EXISTS' : 'MISSING');
    console.log('Auction ID:', auctionId);
    console.log('Username:', currentUsername);
    console.log('All localStorage keys:', Object.keys(localStorage));
    console.log('========================');

    if (!token || !auctionId) {
        console.error('CRITICAL: Token hoặc Auction ID không tồn tại');
        showNotification('error', 'Vui lòng đăng nhập lại để đấu giá.');
        return;
    }

    if (!currentUsername) {
        console.error('CRITICAL: Username không tồn tại');
        showNotification('error', 'Không thể xác định tên người dùng. Vui lòng đăng nhập lại.');
        return;
    }

    // Kiểm tra các element HTML cần thiết
    console.log('=== HTML ELEMENTS CHECK ===');
    const bidButtons = document.querySelectorAll('.bid-button');
    const quickBidButtons = document.querySelectorAll('.quick-bid-btn');
    console.log('Bid buttons found:', bidButtons.length);
    console.log('Quick bid buttons found:', quickBidButtons.length);

    bidButtons.forEach((button, index) => {
        const productId = button.dataset.productId;
        const bidInput = document.getElementById(`bidAmount_${productId}`);
        const priceElement = document.getElementById(`currentPrice_${productId}`);
        console.log(`Button ${index}: ProductID=${productId}, Input=${!!bidInput}, Price=${!!priceElement}`);
    });
    console.log('==============================');

    // Kết nối Socket.IO
    const SOCKET_URL = 'http://localhost:9090';
    console.log('Connecting to socket:', SOCKET_URL);

    const socket = io(SOCKET_URL, {
        query: { token, auctionId },
        transports: ['websocket', 'polling'],
        timeout: 5000,
        forceNew: true
    });

    // Hàm cập nhật trạng thái kết nối
    function updateConnectionStatus(connected) {
        const statusEl = document.getElementById('connectionStatus');
        const textEl = statusEl?.querySelector('.status-text');
        
        if (statusEl) {
            statusEl.className = `connection-status ${connected ? 'connected' : 'disconnected'}`;
        }
        if (textEl) {
            textEl.textContent = connected ? 'Đã kết nối' : 'Mất kết nối';
        }
    }

    // Hàm cập nhật giá sản phẩm và người đặt giá
    function updateProductPrice(productId, amount, username) {
        const priceElement = document.getElementById(`currentPrice_${productId}`);
        const lastBidderElement = document.getElementById(`lastBidder_${productId}`);
        
        if (priceElement && amount) {
            priceElement.textContent = `${amount.toLocaleString('vi-VN')}đ`;
            priceElement.style.animation = 'priceUpdate 0.5s ease-in-out';
            setTimeout(() => {
                priceElement.style.animation = '';
            }, 500);
        }
        
        if (lastBidderElement && username) {
            lastBidderElement.textContent = `Đấu giá bởi: ${username}`;
        } else if (lastBidderElement) {
            lastBidderElement.textContent = '';
        }
    }

    // Hàm hiển thị người thắng cuộc
    function showWinner(productId, winnerName, winningAmount) {
        const bidInterface = document.getElementById(`bidInterface_${productId}`);
        const winnerDisplay = document.getElementById(`winnerDisplay_${productId}`);
        const statusBadge = document.querySelector(`.product-item[data-product-id="${productId}"] .status-badge`);
        
        if (bidInterface) {
            bidInterface.style.display = 'none';
        }
        
        if (winnerDisplay) {
            const winnerNameEl = winnerDisplay.querySelector('.winner-name');
            const winningAmountEl = winnerDisplay.querySelector('.winning-amount');
            
            if (winnerNameEl) winnerNameEl.textContent = winnerName || 'Không có người thắng';
            if (winningAmountEl) winningAmountEl.textContent = winningAmount ? 
                `${winningAmount.toLocaleString('vi-VN')}đ` : '0đ';
            
            winnerDisplay.style.display = 'block';
            winnerDisplay.style.animation = 'slideDown 0.5s ease-out';
        }

        if (statusBadge) {
            statusBadge.textContent = 'COMPLETED';
            statusBadge.className = 'status-badge completed';
        }
    }

    // Hàm cập nhật trạng thái sản phẩm
    function updateProductStatus(productId, status) {
        const statusBadge = document.querySelector(`.product-item[data-product-id="${productId}"] .status-badge`);
        const bidInterface = document.getElementById(`bidInterface_${productId}`);
        if (statusBadge) {
            statusBadge.textContent = status;
            statusBadge.className = `status-badge ${status.toLowerCase()}`;
        }
        if (bidInterface && status !== 'ACTIVE') {
            bidInterface.style.display = 'none';
        }
    }

    socket.on('connect', () => {
        console.log('✅ Socket.IO connected successfully');
        console.log('Socket ID:', socket.id);
        console.log('Room:', auctionId);
        updateConnectionStatus(true);
        socket.emit('requestAuctionDetails', { auctionId });
    });

    socket.on('connect_error', (error) => {
        console.error('❌ Socket connection error:', error);
        updateConnectionStatus(false);
        showNotification('error', `Lỗi kết nối: ${error.message || error}`);
    });

    socket.on('disconnect', (reason) => {
        console.warn('⚠️ Socket disconnected:', reason);
        updateConnectionStatus(false);
    });

    socket.on('initialAuctionDetails', (products) => {
        console.log('📊 Initial auction details received:', products);
        products.forEach(product => {
            const priceElement = document.getElementById(`currentPrice_${product.id}`);
            const lastBidderElement = document.getElementById(`lastBidder_${product.id}`);
            const bidInput = document.getElementById(`bidAmount_${product.id}`);
            const bidButton = document.querySelector(`.bid-button[data-product-id="${product.id}"]`);

            if (priceElement && product.currentPrice) {
                updateProductPrice(product.id, product.currentPrice, product.winnerUsername || null);
                if (bidButton) {
                    bidButton.dataset.currentPrice = product.currentPrice;
                    bidButton.dataset.minIncrement = product.minimumBidIncrement;
                }
                console.log(`✅ Initial price updated for product ${product.id}: ${product.currentPrice}`);
            }

            if (bidInput && product.currentPrice && product.minimumBidIncrement) {
                const newMinBid = product.currentPrice + product.minimumBidIncrement;
                bidInput.dataset.minBid = newMinBid;
                bidInput.min = newMinBid;
                const bidHint = bidInput.closest('.bid-controls')?.querySelector('.bid-hint span');
                if (bidHint) {
                    bidHint.textContent = `${newMinBid.toLocaleString('vi-VN')}đ`;
                }
                console.log(`✅ Initial min bid updated for product ${product.id}: ${newMinBid}`);
            }

            if (product.status) {
                updateProductStatus(product.id, product.status);
            }

            if (product.status === 'COMPLETED' && product.winnerUsername) {
                showWinner(product.id, product.winnerUsername, product.currentPrice);
            }
        });
    });

    socket.on('bidUpdate', (data) => {
        console.log('📈 Bid update received:', data);
        const priceElement = document.getElementById(`currentPrice_${data.productId}`);
        const historyList = document.getElementById(`bidHistory_${data.productId}`);
        const bidCountElement = document.getElementById(`bidCount_${data.productId}`);
        const totalBidsElement = document.getElementById(`totalBids_${data.productId}`);
        const lastBidderElement = document.getElementById(`lastBidder_${data.productId}`);
        const bidInput = document.getElementById(`bidAmount_${data.productId}`);
        const bidButton = document.querySelector(`.bid-button[data-product-id="${data.productId}"]`);
        const uniqueBiddersElement = document.getElementById(`uniqueBidders_${data.productId}`);

        if (priceElement) {
            updateProductPrice(data.productId, data.amount, data.username);
            if (bidButton) {
                bidButton.dataset.currentPrice = data.amount;
            }
            console.log('✅ Price updated for product:', data.productId);
        }

        if (historyList) {
            const noBids = historyList.querySelector('.no-bids');
            if (noBids) noBids.remove();
            
            const li = document.createElement('li');
            li.innerHTML = `
                <strong>${data.username}</strong> - 
                <span class="bid-amount">${data.amount.toLocaleString('vi-VN')}đ</span> 
                <small>(${new Date().toLocaleTimeString('vi-VN')} ${new Date().toLocaleDateString('vi-VN')})</small>
            `;
            li.style.cssText = 'background: #e8f5e8; border-left: 3px solid #4CAF50; margin: 2px 0; padding: 8px;';
            historyList.appendChild(li);
            historyList.scrollTop = historyList.scrollHeight;
            console.log('✅ Bid history updated for product:', data.productId);
        }

        if (bidCountElement && totalBidsElement) {
            const currentCount = parseInt(totalBidsElement.textContent) || 0;
            totalBidsElement.textContent = `${currentCount + 1}`;
            bidCountElement.textContent = `${currentCount + 1} lượt`;
        }

        if (lastBidderElement) {
            lastBidderElement.textContent = `Đấu giá bởi: ${data.username}`;
        }

        if (uniqueBiddersElement && data.uniqueBidders != null) {
            uniqueBiddersElement.textContent = `${data.uniqueBidders}`;
            console.log(`✅ Unique bidders updated for product ${data.productId}: ${data.uniqueBidders}`);
        }

        if (bidInput && data.amount && bidButton) {
            const minimumBidIncrement = parseFloat(bidButton.dataset.minIncrement) || 0;
            const newMinBid = data.amount + minimumBidIncrement;
            bidInput.dataset.minBid = newMinBid;
            bidInput.min = newMinBid;
            const bidHint = bidInput.closest('.bid-controls')?.querySelector('.bid-hint span');
            if (bidHint) {
                bidHint.textContent = `${newMinBid.toLocaleString('vi-VN')}đ`;
            }
            console.log(`✅ Min bid updated for product ${data.productId}: ${newMinBid}`);
        }
    });

    socket.on('outbidNotification', (data) => {
        if (data.targetUsername === currentUsername) {
            console.log('🚨 Outbid notification:', data.message);
            showNotification('warning', `Bạn đã bị trả giá cao hơn! ${data.message}`);
        }
    });

    socket.on('auctionDetails', (data) => {
        console.log('📊 Auction details received:', data);
        if (data.endTime) {
            const endTime = new Date(data.endTime);
            console.log('⏰ Starting countdown to:', endTime.toLocaleString('vi-VN'));
            startCountdown(endTime);
        }
    });

    socket.on('auctionEnded', (data) => {
        console.log('🏁 Auction ended:', data);
        const banner = document.getElementById('auctionEndedBanner');
        if (banner) {
            banner.style.display = 'block';
        }
        bidButtons.forEach(button => {
            button.disabled = true;
            button.querySelector('.btn-text').textContent = 'Đã kết thúc';
            button.style.background = '#95a5a6';
        });
    });

    socket.on('productEnded', (data) => {
        console.log('🏁 Product ended:', data);
        updateProductStatus(data.productId, 'COMPLETED');
        showWinner(data.productId, data.winnerUsername, data.winningAmount);
        showNotification('info', `Sản phẩm ${data.productId} đã kết thúc! ${data.winnerUsername ? 'Người thắng: ' + data.winnerUsername : 'Không có người thắng'}.`);
    });

    socket.on('error', (error) => {
        console.error('❌ Socket error:', error);
        showNotification('error', `Lỗi kết nối: ${error.message || error}`);
    });


    function startCountdown(endTime) {
    console.log('⏰ Starting countdown timer');
    const countdownElements = {
        days: document.getElementById('countdown-days'),
        hours: document.getElementById('countdown-hours'),
        minutes: document.getElementById('countdown-minutes'),
        seconds: document.getElementById('countdown-seconds')
    };
    
    const missingElements = Object.entries(countdownElements)
        .filter(([key, element]) => !element)
        .map(([key]) => key);
    if (missingElements.length > 0) {
        console.error('❌ Missing countdown elements:', missingElements);
        return;
    }

    // Xóa bộ đếm thời gian hiện tại nếu tồn tại
    if (window.countdownInterval) {
        clearInterval(window.countdownInterval);
    }

    const countdown = () => {
        const now = new Date();
        const diff = endTime - now;
        if (diff <= 0) {
            console.log('⏰ Auction ended');
            Object.values(countdownElements).forEach(el => el.textContent = '00');
            showNotification('info', 'Phiên đấu giá đã kết thúc!');
            document.getElementById('auctionEndedBanner').style.display = 'block';
            bidButtons.forEach(button => {
                button.disabled = true;
                button.querySelector('.btn-text').textContent = 'Đã kết thúc';
                button.style.background = '#95a5a6';
            });
            clearInterval(window.countdownInterval); // Xóa bộ đếm khi phiên đấu giá kết thúc
            return;
        }
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        countdownElements.days.textContent = String(days).padStart(2, '0');
        countdownElements.hours.textContent = String(hours).padStart(2, '0');
        countdownElements.minutes.textContent = String(minutes).padStart(2, '0');
        countdownElements.seconds.textContent = String(seconds).padStart(2, '0');
    };

    countdown();
    window.countdownInterval = setInterval(countdown, 1000);
    window.addEventListener('beforeunload', () => clearInterval(window.countdownInterval));
}

    // Xử lý bid buttons
    bidButtons.forEach(button => {
        button.addEventListener('click', (e) => {
            const productId = e.target.closest('.bid-button').dataset.productId;
            const amountInput = document.getElementById(`bidAmount_${productId}`);
            const btnSpinner = button.querySelector('.btn-spinner');
            const btnText = button.querySelector('.btn-text');

            if (!amountInput) {
                console.error('❌ Bid input not found for product:', productId);
                showNotification('error', 'Không tìm thấy trường nhập giá');
                return;
            }

            const amount = parseFloat(amountInput.value.replace(/[^\d]/g, ''));
            console.log('🎯 Bid attempt:', { productId, amount, username: currentUsername, auctionId });

            if (!amount || isNaN(amount) || amount <= 0) {
                showNotification('warning', 'Vui lòng nhập số tiền hợp lệ lớn hơn 0.');
                return;
            }

            const minBid = parseFloat(amountInput.dataset.minBid);
            if (amount < minBid) {
                showNotification('warning', `Số tiền phải lớn hơn hoặc bằng ${minBid.toLocaleString('vi-VN')}đ`);
                return;
            }

            button.disabled = true;
            btnText.textContent = 'Đang xử lý...';
            btnSpinner.style.display = 'inline-block';

            socket.emit('placeBid', {
                productId: parseInt(productId),
                amount: amount,
                username: currentUsername,
                auctionId: auctionId
            }, (response) => {
                button.disabled = false;
                btnText.textContent = 'Đặt Giá';
                btnSpinner.style.display = 'none';
                let message = response;
                if (typeof response === 'object' && response.message) {
                    message = response.message;
                }
                message = (message || '').trim();
                if (message.toLowerCase() === 'bid placed successfully') {
                    showNotification('success', 'Đặt giá thành công!');
                    const userBidCount = document.getElementById('userBidCount');
                    if (userBidCount) {
                        const currentCount = parseInt(userBidCount.textContent) || 0;
                        userBidCount.textContent = `${currentCount + 1} lượt đấu giá`;
                    }
                }
            });

            amountInput.value = '';
            setTimeout(() => {
                if (button.disabled) {
                    button.disabled = false;
                    btnText.textContent = 'Đặt Giá';
                    btnSpinner.style.display = 'none';
                    showNotification('warning', 'Yêu cầu đặt giá hết thời gian phản hồi.');
                }
            }, 5000);
        });
    });

    // Xử lý quick bid buttons
    quickBidButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const productId = btn.dataset.productId;
            const increment = parseInt(btn.dataset.increment || '1');
            const bidInput = document.getElementById(`bidAmount_${productId}`);
            const currentPriceElement = document.getElementById(`currentPrice_${productId}`);
            const bidButton = document.querySelector(`.bid-button[data-product-id="${productId}"]`);

            if (!bidInput || !currentPriceElement || !bidButton) {
                console.error('Bid input, price element, or bid button not found for product:', productId);
                showNotification('error', 'Không tìm thấy phần tử đấu giá');
                return;
            }

            try {
                const currentPriceText = currentPriceElement.textContent || currentPriceElement.innerText;
                const currentPrice = parseFloat(currentPriceText.replace(/[^\d]/g, ''));
                const minimumBidIncrement = parseFloat(bidButton.dataset.minIncrement) || 0;
                const newMinBid = currentPrice + minimumBidIncrement;
                const suggestedBid = currentPrice + (minimumBidIncrement * increment);

                bidInput.value = Math.floor(suggestedBid).toLocaleString('vi-VN');
                bidInput.dataset.minBid = newMinBid;
                bidInput.min = newMinBid;
                const bidHint = bidInput.closest('.bid-controls')?.querySelector('.bid-hint span');
                if (bidHint) {
                    bidHint.textContent = `${newMinBid.toLocaleString('vi-VN')}đ`;
                }
                bidInput.focus();
                console.log('Quick bid set:', { currentPrice, suggestedBid, newMinBid });
            } catch (error) {
                console.error('Error calculating quick bid:', error);
                showNotification('error', 'Lỗi khi tính toán giá đặt nhanh');
            }
        });
    });

    function showNotification(type, message) {
        const existing = document.querySelector('.custom-notification');
        if (existing) existing.remove();
        const notification = document.createElement('div');
        notification.className = `custom-notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span>${message}</span>
                <button onclick="this.parentElement.parentElement.remove()">×</button>
            </div>
        `;

        const styles = {
            error: 'background: #fee; border-left: 4px solid #e74c3c; color: #721c24;',
            warning: 'background: #fff4e6; border-left: 4px solid #f39c12; color: #8a6d3b;',
            success: 'background: #eef; border-left: 4px solid #27ae60; color: #155724;',
            info: 'background: #e6f3ff; border-left: 4px solid #3498db; color: #31708f;'
        };

        notification.style.cssText = `
            position: fixed; top: 20px; right: 20px; z-index: 1000; 
            padding: 15px; border-radius: 4px; max-width: 300px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            ${styles[type]}
        `;

        notification.querySelector('.notification-content').style.cssText = `
            display: flex; justify-content: space-between; align-items: center;
        `;

        notification.querySelector('button').style.cssText = `
            background: none; border: none; font-size: 18px; cursor: pointer; 
            margin-left: 10px; opacity: 0.7;
        `;

        document.body.appendChild(notification);

        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }

    setInterval(() => {
        if (socket.connected) {
            console.log('🔄 Socket still connected');
            updateConnectionStatus(true);
        } else {
            console.warn('⚠️ Socket disconnected, attempting to reconnect...');
            updateConnectionStatus(false);
            socket.connect();
        }
    }, 30000);
});