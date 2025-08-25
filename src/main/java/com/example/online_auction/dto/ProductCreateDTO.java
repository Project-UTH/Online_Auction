package com.example.online_auction.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalTime; // Thay đổi từ LocalDateTime

@Data
public class ProductCreateDTO {
    private String name;
    private String description;
    private Double startingPrice;
    private Double minimumBidIncrement;
    private String imageUrl;
    private MultipartFile imageFile;
    private LocalTime startTime; // Thay đổi thành LocalTime
    private LocalTime endTime; // Thay đổi thành LocalTime
}