package com.example.online_auction.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class ProductCreateDTO {
    private String name; // Tên sản phẩm
    private String description; // Mô tả
    private Double startingPrice; // Giá khởi điểm
    private Double minimumBidIncrement; // Tăng giá tối thiểu
    private String imageUrl; // URL ảnh (sẽ được cập nhật sau khi upload)
    private MultipartFile imageFile; // File ảnh được upload
    private LocalDateTime startTime; // Thời gian bắt đầu sản phẩm
    private LocalDateTime endTime; // Thời gian kết thúc sản phẩm
}