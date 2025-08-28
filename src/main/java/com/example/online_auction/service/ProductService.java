package com.example.online_auction.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Bid;
import com.example.online_auction.entity.Product;
import com.example.online_auction.repository.AuctionRepository;
import com.example.online_auction.repository.BidRepository;
import com.example.online_auction.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // Add RedisTemplate

    @Transactional
    @Scheduled(fixedRate = 30000) // Chạy mỗi 30 giây
    public void updateAllProductStatuses() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::updateProductStatus);
    }

    @Transactional
    public void updateProductStatus(Product product) {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = product.getAuction();
        if (auction != null) {
            LocalDateTime startDateTime = auction.getAuctionDate().atTime(product.getStartTime());
            LocalDateTime endDateTime = auction.getAuctionDate().atTime(product.getEndTime());
            Product.Status newStatus = product.getStatus();

            if (now.isBefore(startDateTime)) {
                newStatus = Product.Status.PENDING;
            } else if (now.isAfter(endDateTime)) {
                newStatus = Product.Status.COMPLETED;
                if (product.getStatus() != Product.Status.COMPLETED) {
                    Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
                    if (highestBid != null) {
                        product.setWinner(highestBid.getUser());
                        String productKey = "auction:" + auction.getId() + ":product_highest_bid:" + product.getId();
                        redisTemplate.opsForValue().set(productKey, highestBid.getAmount());
                    } else {
                        product.setWinner(null);
                    }
                    productRepository.save(product);
                    String auctionId = String.valueOf(auction.getId());
                    socketIOServer.getRoomOperations(auctionId).sendEvent("productEnded", 
                        new BidService.ProductEndedMessage(
                            product.getId(),
                            highestBid != null ? highestBid.getUser().getUsername() : null,
                            highestBid != null ? highestBid.getAmount() : null
                        ));
                    System.out.println("✅ Product " + product.getId() + " ended. Winner: " + 
                        (highestBid != null ? highestBid.getUser().getUsername() : "null") + 
                        ", Amount: " + (highestBid != null ? highestBid.getAmount() : "null"));
                }
            } else {
                newStatus = Product.Status.ACTIVE;
            }

            if (newStatus != product.getStatus()) {
                product.setStatus(newStatus);
                productRepository.save(product);
            }
        }
    }

    @Transactional
    public Product addProductToAuction(Long auctionId, ProductCreateDTO productDTO) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá với ID: " + auctionId));

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setStartingPrice(productDTO.getStartingPrice());
        product.setMinimumBidIncrement(productDTO.getMinimumBidIncrement());
        product.setImageUrl(productDTO.getImageUrl());
        product.setStartTime(productDTO.getStartTime());
        product.setEndTime(productDTO.getEndTime());
        product.setStatus(Product.Status.PENDING);
        product.setAuction(auction);

        return productRepository.save(product);
    }
    public List<Product> getWonProductsByUsername(String username) {
        List<Product> products = productRepository.findByWinnerUsernameAndStatus(username, Product.Status.COMPLETED);
        for (Product product : products) {
            List<Bid> bids = bidRepository.findByProductOrderByTimestampDesc(product);
            if (!bids.isEmpty()) {
                product.setCurrentPrice(bids.get(0).getAmount());
                product.setLastBidder(bids.get(0).getUser().getUsername());
                product.setBidHistory(bids);
            } else {
                product.setCurrentPrice(product.getStartingPrice());
                product.setLastBidder(null);
                product.setBidHistory(List.of());
            }
            product.setBidCount(bidRepository.countByProduct(product));
            long uniqueBidders = bids.stream()
                                     .map(bid -> bid.getUser().getId())
                                     .distinct()
                                     .count();
            product.setUniqueBidders(uniqueBidders);
        }
        return products;
    }
}