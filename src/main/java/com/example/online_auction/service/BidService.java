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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    @Transactional(readOnly = true)
    public void initializeRedisData() {
        System.out.println("Initializing Redis data for active auctions...");
        List<Auction> activeAuctions = auctionRepository.findAll().stream()
                .filter(auction -> auction.getStatus() == Auction.Status.ACTIVE)
                .toList();

        for (Auction auction : activeAuctions) {
            String auctionId = String.valueOf(auction.getId());
            List<Product> products = productRepository.findByAuctionId(auction.getId());
            for (Product product : products) {
                String productKey = AUCTION_KEY_PREFIX + auctionId + PRODUCT_HIGHEST_BID_SUFFIX + product.getId();
                Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
                Double currentPrice = highestBid != null ? highestBid.getAmount() : product.getStartingPrice();
                redisTemplate.opsForValue().set(productKey, currentPrice);
                System.out.println("Initialized product " + product.getId() + " with price: " + currentPrice);
            }
            String participantsKey = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
            if (redisTemplate.opsForValue().get(participantsKey) == null) {
                redisTemplate.opsForValue().set(participantsKey, new HashSet<String>());
            }
        }
        System.out.println("Redis initialization completed");
    }

    @Transactional
    public CompletableFuture<Void> placeBid(String auctionId, BidDTO bidDto) {
        try {
            System.out.println("=== PROCESSING BID ===");
            System.out.println("AuctionId: " + auctionId);
            System.out.println("ProductId: " + bidDto.getProductId());
            System.out.println("Amount: " + bidDto.getAmount());
            System.out.println("Username: " + bidDto.getUsername());

            Auction auction = auctionRepository.findById(Long.parseLong(auctionId))
                    .orElseThrow(() -> new RuntimeException("Phiên đấu giá không tồn tại"));

            if (auction.getStatus() != Auction.Status.ACTIVE) {
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.");
                return CompletableFuture.completedFuture(null);
            }

            Product product = productRepository.findById(bidDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            if (!product.getAuction().getId().equals(Long.parseLong(auctionId))) {
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", "Sản phẩm không thuộc phiên đấu giá này");
                return CompletableFuture.completedFuture(null);
            }

            if (product.getStatus() != Product.Status.ACTIVE) {
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", "Sản phẩm chưa sẵn sàng đấu giá.");
                return CompletableFuture.completedFuture(null);
            }

            User user = userRepository.findByUsername(bidDto.getUsername())
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            String productKey = AUCTION_KEY_PREFIX + auctionId + PRODUCT_HIGHEST_BID_SUFFIX + bidDto.getProductId();
            Double currentHighestBid = (Double) redisTemplate.opsForValue().get(productKey);

            if (currentHighestBid == null) {
                Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
                currentHighestBid = highestBid != null ? highestBid.getAmount() : product.getStartingPrice();
                redisTemplate.opsForValue().set(productKey, currentHighestBid);
            }

            System.out.println("Current highest bid: " + currentHighestBid);
            System.out.println("Minimum increment: " + product.getMinimumBidIncrement());

            if (bidDto.getAmount() <= currentHighestBid) {
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", 
                    "Giá đấu phải lớn hơn giá hiện tại: " + currentHighestBid.intValue() + "đ");
                return CompletableFuture.completedFuture(null);
            }

            if ((bidDto.getAmount() - currentHighestBid) < product.getMinimumBidIncrement()) {
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", 
                    "Tăng giá tối thiểu: " + product.getMinimumBidIncrement().intValue() + "đ");
                return CompletableFuture.completedFuture(null);
            }

            Bid bid = new Bid();
            bid.setAmount(bidDto.getAmount());
            bid.setUser(user);
            bid.setProduct(product);
            bid.setTimestamp(LocalDateTime.now());
            bidRepository.save(bid);

            redisTemplate.opsForValue().set(productKey, bidDto.getAmount());

            String participantsKey = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
            @SuppressWarnings("unchecked")
            Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(participantsKey);
            if (participants == null) participants = new HashSet<>();
            participants.add(bidDto.getUsername());
            redisTemplate.opsForValue().set(participantsKey, participants);

            if (socketIOServer != null) {
                // Tính số người tham gia duy nhất từ database
                List<Bid> bidHistory = bidRepository.findByProductOrderByTimestampDesc(product);
                long uniqueBidders = bidHistory.stream()
                        .map(b -> b.getUser().getUsername())
                        .distinct()
                        .count();
                
                BidUpdateMessage bidUpdate = new BidUpdateMessage(
                    bidDto.getProductId(), 
                    bidDto.getAmount(), 
                    bidDto.getUsername(), 
                    uniqueBidders
                );
                socketIOServer.getRoomOperations(auctionId).sendEvent("bidUpdate", bidUpdate);
                System.out.println("✅ Bid update broadcasted to room: " + auctionId + ", Unique Bidders: " + uniqueBidders);

                for (String participantUsername : participants) {
                    if (!participantUsername.equals(bidDto.getUsername())) {
                        OutbidMessage outbidMsg = new OutbidMessage(
                            bidDto.getProductId(), 
                            participantUsername,
                            "Bạn đã bị vượt giá cho sản phẩm " + product.getName()
                        );
                        socketIOServer.getRoomOperations(auctionId).sendEvent("outbidNotification", outbidMsg);
                    }
                }

                LocalDateTime endTime = auction.getAuctionDate().atTime(auction.getEndTime());
                AuctionDetailsMessage auctionDetails = new AuctionDetailsMessage(endTime.toString());
                socketIOServer.getRoomOperations(auctionId).sendEvent("auctionDetails", auctionDetails);

                ParticipantUpdateMessage participantUpdate = new ParticipantUpdateMessage(auctionId, new ArrayList<>(participants));
                socketIOServer.getRoomOperations(auctionId).sendEvent("participantUpdate", participantUpdate);

                socketIOServer.getRoomOperations(auctionId).sendEvent("bidAck", "Bid placed successfully");
                System.out.println("✅ Bid placed successfully for productId: " + bidDto.getProductId());
            } else {
                System.err.println("❌ SocketIOServer is null, cannot send events");
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            System.err.println("❌ Error processing bid: " + e.getMessage());
            socketIOServer.getRoomOperations(auctionId).sendEvent("bidError", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    public void joinAuction(String auctionId, String username) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants == null) participants = new HashSet<>();
        participants.add(username);
        redisTemplate.opsForValue().set(key, participants);
        System.out.println("User " + username + " joined auction " + auctionId);
    }

    public void leaveAuction(String auctionId, String username) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants != null) {
            participants.remove(username);
            redisTemplate.opsForValue().set(key, participants);
        }
        System.out.println("User " + username + " left auction " + auctionId);
    }

    public Double getCurrentPrice(String auctionId, Long productId) {
        String productKey = AUCTION_KEY_PREFIX + auctionId + PRODUCT_HIGHEST_BID_SUFFIX + productId;
        Double currentPrice = (Double) redisTemplate.opsForValue().get(productKey);
        if (currentPrice == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
            currentPrice = highestBid != null ? highestBid.getAmount() : product.getStartingPrice();
            redisTemplate.opsForValue().set(productKey, currentPrice);
        }
        return currentPrice;
    }

    public List<ProductWithCurrentPrice> getInitialAuctionDetails(String auctionId) {
        Auction auction = auctionRepository.findByIdWithProducts(Long.parseLong(auctionId));
        List<ProductWithCurrentPrice> products = new ArrayList<>();
        for (Product product : auction.getProducts()) {
            String productKey = AUCTION_KEY_PREFIX + auctionId + PRODUCT_HIGHEST_BID_SUFFIX + product.getId();
            Double currentPrice = (Double) redisTemplate.opsForValue().get(productKey);
            if (currentPrice == null) {
                Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
                currentPrice = highestBid != null ? highestBid.getAmount() : product.getStartingPrice();
                redisTemplate.opsForValue().set(productKey, currentPrice);
            }
            products.add(new ProductWithCurrentPrice(
                product.getId(),
                product.getName(),
                currentPrice,
                product.getMinimumBidIncrement(),
                product.getStatus().toString(),
                product.getWinner() != null ? product.getWinner().getUsername() : null
            ));
        }
        return products;
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkAuctionEnd() {
        List<Auction> activeAuctions = auctionRepository.findAllWithProducts().stream()
                .filter(auction -> auction.getStatus() == Auction.Status.ACTIVE)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions) {
            LocalDateTime endTime = auction.getAuctionDate().atTime(auction.getEndTime());
            if (now.isAfter(endTime)) {
                auction.setStatus(Auction.Status.COMPLETED);
                auctionRepository.save(auction);
                for (Product product : auction.getProducts()) {
                    if (product.getStatus() == Product.Status.ACTIVE) {
                        product.setStatus(Product.Status.COMPLETED);
                        Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
                        if (highestBid != null) {
                            product.setWinner(highestBid.getUser());
                        }
                        productRepository.save(product);
                        String auctionId = String.valueOf(auction.getId());
                        socketIOServer.getRoomOperations(auctionId).sendEvent("productEnded", 
                            new ProductEndedMessage(
                                product.getId(),
                                highestBid != null ? highestBid.getUser().getUsername() : null,
                                highestBid != null ? highestBid.getAmount() : null
                            ));
                    }
                }
                socketIOServer.getRoomOperations(String.valueOf(auction.getId())).sendEvent("auctionEnded", 
                    new AuctionEndedMessage(String.valueOf(auction.getId()), null, null));
            }
        }
    }

    public static class ProductWithCurrentPrice {
        private Long id;
        private String name;
        private Double currentPrice;
        private Double minimumBidIncrement;
        private String status;
        private String winnerUsername;

        public ProductWithCurrentPrice(Long id, String name, Double currentPrice, Double minimumBidIncrement, String status, String winnerUsername) {
            this.id = id;
            this.name = name;
            this.currentPrice = currentPrice;
            this.minimumBidIncrement = minimumBidIncrement;
            this.status = status;
            this.winnerUsername = winnerUsername;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Double getCurrentPrice() { return currentPrice; }
        public Double getMinimumBidIncrement() { return minimumBidIncrement; }
        public String getStatus() { return status; }
        public String getWinnerUsername() { return winnerUsername; }
    }

    public static class BidUpdateMessage {
        private Long productId;
        private Double amount;
        private String username;
        private long uniqueBidders;

        public BidUpdateMessage(Long productId, Double amount, String username, long uniqueBidders) {
            this.productId = productId;
            this.amount = amount;
            this.username = username;
            this.uniqueBidders = uniqueBidders;
        }

        public Long getProductId() { return productId; }
        public Double getAmount() { return amount; }
        public String getUsername() { return username; }
        public long getUniqueBidders() { return uniqueBidders; }
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

        public Long getProductId() { return productId; }
        public String getTargetUsername() { return targetUsername; }
        public String getMessage() { return message; }
    }

    public static class AuctionEndedMessage {
        private String auctionId;
        private Long productId;
        private String winnerUsername;

        public AuctionEndedMessage(String auctionId, Long productId, String winnerUsername) {
            this.auctionId = auctionId;
            this.productId = productId;
            this.winnerUsername = winnerUsername;
        }

        public String getAuctionId() { return auctionId; }
        public Long getProductId() { return productId; }
        public String getWinnerUsername() { return winnerUsername; }
    }

    public static class ProductEndedMessage {
        private Long productId;
        private String winnerUsername;
        private Double winningAmount;

        public ProductEndedMessage(Long productId, String winnerUsername, Double winningAmount) {
            this.productId = productId;
            this.winnerUsername = winnerUsername;
            this.winningAmount = winningAmount;
        }

        public Long getProductId() { return productId; }
        public String getWinnerUsername() { return winnerUsername; }
        public Double getWinningAmount() { return winningAmount; }
    }

    public static class AuctionDetailsMessage {
        private String endTime;

        public AuctionDetailsMessage(String endTime) {
            this.endTime = endTime;
        }

        public String getEndTime() { return endTime; }
    }

    public static class ParticipantUpdateMessage {
        private String auctionId;
        private List<String> participants;

        public ParticipantUpdateMessage(String auctionId, List<String> participants) {
            this.auctionId = auctionId;
            this.participants = participants;
        }

        public String getAuctionId() { return auctionId; }
        public List<String> getParticipants() { return participants; }
    }
}