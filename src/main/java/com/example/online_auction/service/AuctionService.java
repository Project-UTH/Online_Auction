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

import java.util.ArrayList;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProductRepository productRepository;

    public Auction createAuction(AuctionCreateDTO dto, User admin) {
        Auction auction = new Auction();
        auction.setName(dto.getName());
        auction.setAuctionDate(dto.getStartTime().toLocalDate());
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
}