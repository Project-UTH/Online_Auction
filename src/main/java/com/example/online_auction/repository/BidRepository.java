package com.example.online_auction.repository;

import com.example.online_auction.entity.Bid;
import com.example.online_auction.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    // Tìm bid cao nhất cho một sản phẩm
    Bid findTopByProductOrderByAmountDesc(Product product);
    
    // Tìm tất cả bid cho một sản phẩm, sắp xếp theo thời gian
    List<Bid> findByProductOrderByTimestampDesc(Product product);
    
    // Tìm tất cả bid của một user
    List<Bid> findByUserUsernameOrderByTimestampDesc(String username);
    
    // Tìm bid cao nhất cho mỗi sản phẩm trong một auction
    @Query("SELECT b FROM Bid b WHERE b.product.auction.id = :auctionId " +
           "AND b.amount = (SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.product.id = b.product.id)")
    List<Bid> findHighestBidsByAuctionId(@Param("auctionId") Long auctionId);
    
    // Đếm số lượng bid cho một sản phẩm
    long countByProduct(Product product);
    
    // Tìm bid của user cho một sản phẩm cụ thể
    List<Bid> findByProductAndUserUsernameOrderByTimestampDesc(Product product, String username);
}