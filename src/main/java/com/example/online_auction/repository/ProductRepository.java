package com.example.online_auction.repository;

import com.example.online_auction.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
       // Thêm nếu cần
    @Query("SELECT p FROM Product p WHERE p.auction.id = ?1")
    List<Product> findByAuctionId(Long auctionId);

    @Query("SELECT p FROM Product p WHERE p.auction.id = :auctionId AND p.id != :id " +
           "AND ((p.startTime < :endTime AND p.endTime > :startTime) OR " +
           "(p.endTime > :startTime AND p.startTime < :endTime))")
    List<Product> findOverlappingProducts(@Param("auctionId") Long auctionId,
                                         @Param("id") Long id,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime);
}