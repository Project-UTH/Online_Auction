package com.example.online_auction.service;

import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Product;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.AuctionRepository;
import com.example.online_auction.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    public Product addProductToAuction(Long auctionId, ProductCreateDTO dto) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá"));

        // Validate auction status
        if (auction.getStatus() != Auction.Status.PENDING) {
            throw new RuntimeException("Chỉ có thể thêm sản phẩm vào phiên đấu giá ở trạng thái PENDING.");
        }

        // Validate startTime before endTime
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("Thời gian bắt đầu của sản phẩm phải trước thời gian kết thúc.");
        }

        // Validate product times are within auction times
        if (dto.getStartTime().isBefore(auction.getStartTime()) || 
            dto.getEndTime().isAfter(auction.getEndTime())) {
            throw new RuntimeException("Thời gian sản phẩm phải nằm trong khoảng thời gian của phiên đấu giá.");
        }

        // Validate for overlapping products
        List<Product> overlapping = productRepository.findOverlappingProducts(
                auctionId, -1L, dto.getStartTime(), dto.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Thời gian sản phẩm trùng với sản phẩm khác trong cùng phiên.");
        }

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setStartingPrice(dto.getStartingPrice());
        product.setMinimumBidIncrement(dto.getMinimumBidIncrement());
        product.setImageUrl(dto.getImageUrl());
        product.setStartTime(dto.getStartTime());
        product.setEndTime(dto.getEndTime());
        product.setStatus(Product.Status.PENDING);
        product.setAuction(auction);
        return productRepository.save(product);
    }

    /**
     * Cập nhật trạng thái tất cả sản phẩm định kỳ
     */
    @Scheduled(fixedRate = 30000) // Chạy mỗi 30 giây
    public void updateAllProductStatuses() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::updateProductStatus);
    }

    /**
     * Cập nhật trạng thái sản phẩm dựa trên thời gian thực tế
     */
    public Product updateProductStatus(Product product) {
        Auction auction = product.getAuction();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = auction.getAuctionDate().atTime(product.getStartTime());
        LocalDateTime endDateTime = auction.getAuctionDate().atTime(product.getEndTime());

        if (product.getStatus() == Product.Status.CANCELLED) {
            return product; // Không thay đổi trạng thái nếu đã bị hủy
        }

        Product.Status newStatus = product.getStatus();
        if (now.isBefore(startDateTime)) {
            newStatus = Product.Status.PENDING;
        } else if (now.isAfter(endDateTime)) {
            newStatus = Product.Status.COMPLETED;
        } else {
            newStatus = Product.Status.ACTIVE;
        }

        if (newStatus != product.getStatus()) {
            product.setStatus(newStatus);
            return productRepository.save(product);
        }
        return product;
    }

    /**
     * Đặt giá thầu cho sản phẩm
     */
    public void placeBid(Long productId, Double bidAmount, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        // Cập nhật trạng thái sản phẩm trước khi đặt giá
        updateProductStatus(product);

        // Kiểm tra trạng thái sản phẩm
        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new RuntimeException("Chỉ có thể đặt giá thầu cho sản phẩm đang ở trạng thái ACTIVE.");
        }

        // Kiểm tra giá thầu hợp lệ
        if (bidAmount < product.getStartingPrice()) {
            throw new RuntimeException("Giá thầu phải lớn hơn hoặc bằng giá khởi điểm.");
        }

        // TODO: Thêm logic kiểm tra và lưu giá thầu (ví dụ: lưu vào bảng bids)
        // Ví dụ: bidService.saveBid(product, user, bidAmount);
        System.out.println("Đặt giá thầu " + bidAmount + " cho sản phẩm " + product.getName() + " bởi " + user.getUsername());
    }
}