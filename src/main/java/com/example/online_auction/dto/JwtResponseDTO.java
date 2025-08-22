package com.example.online_auction.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JwtResponseDTO {
    private String token;
    private String role;

    // Thêm constructor với 2 tham số
    public JwtResponseDTO(String token, String role) {
        this.token = token;
        this.role = role;
    }
}