package com.example.online_auction.service;

import com.example.online_auction.dto.AuctionCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    public Auction createAuction(AuctionCreateDTO dto, User admin) {
        // Validate startTime before endTime
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        // Validate for overlapping auctions
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
        // Validate startTime before endTime
        if (!auction.getStartTime().isBefore(auction.getEndTime())) {
            throw new RuntimeException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        return auctionRepository.save(auction);
    }

    /**
     * Cập nhật trạng thái tất cả phiên đấu giá định kỳ
     */
    @Scheduled(fixedRate = 30000) // Chạy mỗi 30 giây
    public void updateAllAuctionStatuses() {
        List<Auction> auctions = auctionRepository.findAll();
        auctions.forEach(this::updateAuctionStatus);
    }

    /**
     * Cập nhật trạng thái phiên đấu giá dựa trên thời gian thực tế
     */
    public Auction updateAuctionStatus(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = auction.getAuctionDate().atTime(auction.getStartTime());
        LocalDateTime endDateTime = auction.getAuctionDate().atTime(auction.getEndTime());

        if (auction.getStatus() == Auction.Status.CANCELLED) {
            return auction; // Không thay đổi trạng thái nếu đã bị hủy
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
}