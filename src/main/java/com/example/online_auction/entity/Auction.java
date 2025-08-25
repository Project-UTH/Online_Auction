package com.example.online_auction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime; // Thay đổi từ LocalDateTime
import java.util.List;

@Entity
@Table(name = "auctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate auctionDate;

    @Column(nullable = false)
    private LocalTime startTime; // Thay đổi thành LocalTime

    @Column(nullable = false)
    private LocalTime endTime; // Thay đổi thành LocalTime

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "auction_participants",
        joinColumns = @JoinColumn(name = "auction_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    public enum Status {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}