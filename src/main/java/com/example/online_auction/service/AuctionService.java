package com.example.online_auction.service;

import com.example.online_auction.dto.AuctionCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {
    @Autowired
    private AuctionRepository auctionRepository;

    public Auction createAuction(AuctionCreateDTO dto, User admin) {
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        List<Auction> overlapping = auctionRepository.findOverlappingAuctions(
                dto.getAuctionDate(), -1L, dto.getStartTime(), dto.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Thời gian phiên đấu giá trùng với phiên khác trong cùng ngày.");
        }
        Auction auction = new Auction();
        auction.setName(dto.getName());
        auction.setAuctionDate(dto.getAuctionDate());
        auction.setStartTime(dto.getStartTime());
        auction.setEndTime(dto.getEndTime());
        auction.setStatus(Auction.Status.PENDING);
        auction.setProducts(new ArrayList<>());
        return auctionRepository.save(auction);
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đấu giá với ID: " + id));
    }

    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    public Auction saveAuction(Auction auction) {
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        return auctionRepository.save(auction);
    }

    @Scheduled(fixedRate = 5000) // Chạy mỗi 30 giây
    @Transactional
    public void updateAllAuctionStatuses() {
        List<Auction> auctions = auctionRepository.findAll();
        auctions.forEach(this::updateAuctionStatus);
    }

    public Auction updateAuctionStatus(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = auction.getAuctionDate().atTime(auction.getStartTime());
        LocalDateTime endDateTime = auction.getAuctionDate().atTime(auction.getEndTime());
        if (auction.getStatus() == Auction.Status.CANCELLED) {
            return auction;
        }
        Auction.Status newStatus = auction.getStatus();
        if (now.isBefore(startDateTime)) {
            newStatus = Auction.Status.PENDING;
        } else if (now.isAfter(endDateTime)) {
            newStatus = Auction.Status.COMPLETED;
        } else {
            newStatus = Auction.Status.ACTIVE;
        }
        if (newStatus != auction.getStatus()) {
            auction.setStatus(newStatus);
            return auctionRepository.save(auction);
        }
        return auction;
    }

    // Thêm phương thức để lấy danh sách ngày có phiên đấu giá
    public List<LocalDate> getAuctionDates() {
        List<Auction> auctions = auctionRepository.findAll();
        System.out.println("All auctions retrieved: " + auctions); // Debug log
        return auctions.stream()
                .map(Auction::getAuctionDate)
                .distinct()
                .toList();
    }

    // Thêm phương thức để lấy danh sách phiên đấu giá theo ngày
    @Transactional(readOnly = true)
    public List<Auction> getAuctionsByDate(LocalDate date) {
        try {
            System.out.println("Fetching auctions for date: " + date); // Debug log
            List<Auction> auctions = auctionRepository.findByAuctionDate(date);
            System.out.println("Raw auctions found for date " + date + ": " + auctions); // Debug log
            return auctions.stream()
                    .filter(auction -> auction.getStatus() == Auction.Status.PENDING || auction.getStatus() == Auction.Status.ACTIVE || auction.getStatus() == Auction.Status.COMPLETED)
                    .toList();
        } catch (Exception e) {
            System.err.println("Error in getAuctionsByDate: " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Trả về danh sách rỗng nếu lỗi
        }
    }
}