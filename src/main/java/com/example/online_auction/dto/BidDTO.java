// src/main/java/com/example/online_auction/dto/BidDto.java (DTO cho bid)
package com.example.online_auction.dto;

import lombok.Data;

@Data
public class BidDTO {
    private Long productId;
    private Double amount;
    private String username; // Set từ server
}