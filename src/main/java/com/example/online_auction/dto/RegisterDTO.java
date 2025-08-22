package com.example.online_auction.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String fullName;
    private String displayName;
    private String address;
    private String phone;
    private String email;
}