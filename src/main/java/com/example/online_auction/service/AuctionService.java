package com.example.online_auction.service;

import com.example.online_auction.dto.AuctionCreateDTO;
import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Product;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.AuctionRepository;
import com.example.online_auction.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProductRepository productRepository;

    public Auction createAuction(AuctionCreateDTO dto, User admin) {
        // Validate overlap với các auctions khác trong cùng ngày
        LocalDate auctionDate = dto.getStartTime().toLocalDate();
        List<Auction> overlapping = auctionRepository.findOverlappingAuctions(
                auctionDate, -1L, dto.getStartTime(), dto.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Thời gian phiên đấu giá trùng lặp với phiên khác trong cùng ngày.");
        }

        Auction auction = new Auction();
        auction.setName(dto.getName());
        auction.setAuctionDate(auctionDate);
        auction.setStartTime(dto.getStartTime());
        auction.setEndTime(dto.getEndTime());
        auction.setStatus(Auction.Status.PENDING);
        auction.setProducts(new ArrayList<>());
        auction = auctionRepository.save(auction);
        return auction;
    }

    public Product addProductToAuction(Long auctionId, ProductCreateDTO dto) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Validate overlap với các products khác trong auction
        List<Product> overlapping = productRepository.findOverlappingProducts(
                auctionId, -1L, dto.getStartTime(), dto.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Thời gian sản phẩm trùng lặp với sản phẩm khác trong phiên.");
        }

        // Validate product time trong auction time
        if (dto.getStartTime().isBefore(auction.getStartTime()) || dto.getEndTime().isAfter(auction.getEndTime())) {
            throw new RuntimeException("Thời gian sản phẩm phải nằm trong thời gian phiên đấu giá.");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setStartingPrice(dto.getStartingPrice());
        product.setMinimumBidIncrement(dto.getMinimumBidIncrement());
        product.setImageUrl(dto.getImageUrl());
        product.setStartTime(dto.getStartTime());
        product.setEndTime(dto.getEndTime());
        product.setStatus(Product.Status.PENDING);
        product.setAuction(auction);
        product = productRepository.save(product);
        auction.getProducts().add(product);
        auctionRepository.save(auction);
        return product;
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id).orElse(null);
    }

    public Auction saveAuction(Auction auction) {
        return auctionRepository.save(auction);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }
}