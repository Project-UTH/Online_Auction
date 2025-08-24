package com.example.online_auction.repository;

import com.example.online_auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime; // Thêm import này
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByAuctionDate(LocalDate date);

    @Query("SELECT a FROM Auction a WHERE a.auctionDate = :date AND a.id != :id " +
           "AND ((a.startTime < :endTime AND a.endTime > :startTime) OR " +
           "(a.endTime > :startTime AND a.startTime < :endTime))")
    List<Auction> findOverlappingAuctions(@Param("date") LocalDate date,
                                          @Param("id") Long id,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}