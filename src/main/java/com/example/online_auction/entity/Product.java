package com.example.online_auction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime; // Thay đổi từ LocalDateTime

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double startingPrice;

    @Column(nullable = false)
    private Double minimumBidIncrement;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private LocalTime startTime; // Thay đổi thành LocalTime

    @Column(nullable = false)
    private LocalTime endTime; // Thay đổi thành LocalTime

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    public enum Status {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}