package com.example.online_auction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String name;  // Tên sản phẩm

    @Column(columnDefinition = "TEXT")
    private String description;  // Mô tả sản phẩm

    @Column(nullable = false)
    private Double startingPrice;  // Giá khởi điểm

    @Column(nullable = false)
    private Double minimumBidIncrement;  // Tăng giá thầu tối thiểu (ví dụ: 10$)

    @Column
    private String imageUrl;  // URL ảnh (ví dụ: /images/products/product-1.jpg, lưu local)

    @Column(nullable = false)
    private LocalDateTime startTime;  // Thời gian bắt đầu đấu giá sản phẩm này

    @Column(nullable = false)
    private LocalDateTime endTime;  // Thời gian kết thúc đấu giá sản phẩm này

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;  // Trạng thái sản phẩm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;  // Người thắng cuộc khi kết thúc (cập nhật tự động)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;  // Liên kết với phiên đấu giá

    public enum Status {
        PENDING,  // Chờ bắt đầu
        ACTIVE,   // Đang đấu giá
        COMPLETED,  // Kết thúc
        CANCELLED  // Hủy
    }
}