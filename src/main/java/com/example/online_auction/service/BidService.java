// src/main/java/com/example/online_auction/service/BidService.java (Cập nhật để không cần start server)
package com.example.online_auction.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.online_auction.dto.BidDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Bid;
import com.example.online_auction.entity.Product;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.AuctionRepository;
import com.example.online_auction.repository.BidRepository;
import com.example.online_auction.repository.ProductRepository;
import com.example.online_auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class BidService {

    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String PRODUCT_HIGHEST_BID_SUFFIX = ":product_highest_bid:";
    private static final String PARTICIPANTS_SUFFIX = ":participants";

    @Async
    @Transactional
    public CompletableFuture<Void> placeBid(String auctionId, BidDTO bidDto) {
        Auction auction = auctionRepository.findById(Long.parseLong(auctionId))
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (auction.getStatus() != Auction.Status.ACTIVE) {
            return CompletableFuture.completedFuture(null);
        }

        Product product = productRepository.findById(bidDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getAuction().getId() != Long.parseLong(auctionId) || product.getStatus() != Product.Status.ACTIVE) {
            return CompletableFuture.completedFuture(null);
        }

        String productKey = AUCTION_KEY_PREFIX + auctionId + PRODUCT_HIGHEST_BID_SUFFIX + bidDto.getProductId();
        Double currentHighestBid = (Double) redisTemplate.opsForValue().get(productKey);
        if (currentHighestBid == null) {
            currentHighestBid = product.getStartingPrice();
        }

        if (bidDto.getAmount() <= currentHighestBid || (bidDto.getAmount() - currentHighestBid) < product.getMinimumBidIncrement()) {
            return CompletableFuture.completedFuture(null);
        }

        User user = userRepository.findByUsername(bidDto.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bid bid = new Bid();
        bid.setAmount(bidDto.getAmount());
        bid.setUser(user);
        bid.setProduct(product);
        bid.setTimestamp(LocalDateTime.now());
        bidRepository.save(bid);

        redisTemplate.opsForValue().set(productKey, bidDto.getAmount());

        socketIOServer.getRoomOperations(auctionId).sendEvent("bidUpdate", 
                new BidUpdateMessage(bidDto.getProductId(), bidDto.getAmount(), bidDto.getUsername()));

        String participantsKey = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(participantsKey);
        if (participants != null) {
            for (String participantUsername : participants) {
                if (!participantUsername.equals(bidDto.getUsername())) {
                    socketIOServer.getRoomOperations(auctionId).sendEvent("outbidNotification",
                            new OutbidMessage(bidDto.getProductId(), participantUsername, "Bạn đã bị vượt giá cho sản phẩm " + product.getName()));
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    public void joinAuction(String auctionId, String username) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants == null) participants = new HashSet<>();
        participants.add(username);
        redisTemplate.opsForValue().set(key, participants);
    }

    public void leaveAuction(String auctionId, String username) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants != null) {
            participants.remove(username);
            redisTemplate.opsForValue().set(key, participants);
        }
    }

    // Classes phụ cho messages
    public static class BidUpdateMessage {
        private Long productId;
        private Double amount;
        private String username;

        public BidUpdateMessage(Long productId, Double amount, String username) {
            this.productId = productId;
            this.amount = amount;
            this.username = username;
        }

        // Getters
        public Long getProductId() { return productId; }
        public Double getAmount() { return amount; }
        public String getUsername() { return username; }
    }

    public static class OutbidMessage {
        private Long productId;
        private String targetUsername;
        private String message;

        public OutbidMessage(Long productId, String targetUsername, String message) {
            this.productId = productId;
            this.targetUsername = targetUsername;
            this.message = message;
        }

        // Getters
        public Long getProductId() { return productId; }
        public String getTargetUsername() { return targetUsername; }
        public String getMessage() { return message; }
    }
}