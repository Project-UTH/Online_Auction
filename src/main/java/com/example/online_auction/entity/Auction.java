package com.example.online_auction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private String name;  // Tên phiên đấu giá (ví dụ: "Phiên đấu giá tháng 8")

    @Column(nullable = false)
    private LocalDate auctionDate;  // Ngày đấu giá tổng thể của phiên

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;  // Danh sách sản phẩm trong phiên (admin tạo trước)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;  // Trạng thái phiên

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "auction_participants",
        joinColumns = @JoinColumn(name = "auction_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;  // Danh sách người tham gia phiên

    public enum Status {
        PENDING,  // Chờ bắt đầu
        ACTIVE,   // Đang diễn ra
        COMPLETED,  // Kết thúc
        CANCELLED  // Hủy
    }
}