package com.example.online_auction.repository;

import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    
    // Tìm các phiên đấu giá theo ngày
    List<Auction> findByAuctionDate(LocalDate date);

    // Tìm các phiên đấu giá có thời gian trùng lặp
    @Query("SELECT a FROM Auction a WHERE a.auctionDate = :date AND a.id != :id " +
           "AND ((a.startTime < :endTime AND a.endTime > :startTime) OR " +
           "(a.endTime > :startTime AND a.startTime < :endTime))")
    List<Auction> findOverlappingAuctions(@Param("date") LocalDate date,
                                         @Param("id") Long id,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime);

    // Tìm tất cả phiên đấu giá sắp xếp theo ngày và giờ
    @Query("SELECT a FROM Auction a ORDER BY a.auctionDate ASC, a.startTime ASC")
    List<Auction> findAllOrderByDateAndTime();

    @Query("SELECT a FROM Auction a JOIN FETCH a.products WHERE a.status = 'ACTIVE'")
    List<Auction> findAllWithProducts();

    @Query("SELECT a FROM Auction a JOIN FETCH a.products WHERE a.id = ?1")
    Auction findByIdWithProducts(Long id);

    // Tìm các phiên đấu giá trong tương lai
    @Query("SELECT a FROM Auction a WHERE a.auctionDate > :currentDate OR " +
           "(a.auctionDate = :currentDate AND a.startTime > :currentTime) " +
           "ORDER BY a.auctionDate ASC, a.startTime ASC")
    List<Auction> findFutureAuctions(@Param("currentDate") LocalDate currentDate, 
                                    @Param("currentTime") LocalTime currentTime);

    // Tìm các phiên đấu giá theo trạng thái
    List<Auction> findByStatus(Auction.Status status);

    // Thêm nếu cần
    @Query("SELECT p FROM Product p WHERE p.auction.id = ?1")
    List<Product> findByAuctionId(Long auctionId);

    // Tìm các phiên đấu giá theo khoảng thời gian
    @Query("SELECT a FROM Auction a WHERE a.auctionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.auctionDate ASC, a.startTime ASC")
    List<Auction> findByDateRange(@Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);
}