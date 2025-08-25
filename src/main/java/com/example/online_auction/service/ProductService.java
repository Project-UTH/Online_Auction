package com.example.online_auction.service;

import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Product;
import com.example.online_auction.repository.AuctionRepository;
import com.example.online_auction.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
}