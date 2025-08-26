package com.example.online_auction.dto;

public class BidDTO {
    private Long productId;
    private Double amount;
    private String username;

    // Constructors
    public BidDTO() {}

    public BidDTO(Long productId, Double amount, String username) {
        this.productId = productId;
        this.amount = amount;
        this.username = username;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "BidDTO{" +
                "productId=" + productId +
                ", amount=" + amount +
                ", username='" + username + '\'' +
                '}';
    }
}