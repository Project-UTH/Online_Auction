package com.example.online_auction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;  // Tên đăng nhập

    @Column(nullable = false)
    private String password;  // Mật khẩu (hash bằng BCrypt trong service)

    @Column(nullable = false)
    private String fullName;  // Họ và tên

    @Column(nullable = false)
    private String displayName;  // Tên hiển thị trong phiên đấu giá

    @Column(nullable = false)
    private String address;  // Địa chỉ

    @Column(nullable = false)
    private String phone;  // Số điện thoại

    @Column(nullable = false, unique = true)
    private String email;  // Email

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // Vai trò: USER hoặc ADMIN

    @Column(nullable = false)
    private boolean verified = false;  // Xác thực email/phone (có thể dùng để gửi xác nhận)

    public enum Role {
        USER, ADMIN
    }
}