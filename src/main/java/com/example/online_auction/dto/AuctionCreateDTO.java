package com.example.online_auction.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime; // Thay đổi từ LocalDateTime
import java.util.List;

@Data
public class AuctionCreateDTO {
    private String name;
    private LocalDate auctionDate; // Thêm trường này
    private LocalTime startTime; // Thay đổi thành LocalTime
    private LocalTime endTime; // Thay đổi thành LocalTime
    private List<ProductCreateDTO> products;
}