package com.example.online_auction.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuctionCreateDTO {
    private String name; // Tên phiên
    private LocalDateTime startTime; // Giờ bắt đầu phiên
    private LocalDateTime endTime; // Giờ kết thúc phiên
    private List<ProductCreateDTO> products; // Danh sách sản phẩm (sẽ add dần)
}