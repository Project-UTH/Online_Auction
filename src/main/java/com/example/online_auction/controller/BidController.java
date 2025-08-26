// src/main/java/com/example/online_auction/controller/BidController.java (File mới cho REST fallback nếu cần, nhưng chính dùng Socket.IO)
package com.example.online_auction.controller;

import com.example.online_auction.dto.BidDTO;
import com.example.online_auction.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bid")
public class BidController {

    @Autowired
    private BidService bidService;

    // Fallback REST cho bid nếu Socket.IO fail, nhưng khuyến nghị dùng Socket
    @PostMapping("/{auctionId}")
    public ResponseEntity<String> placeBidFallback(@PathVariable String auctionId, @RequestBody BidDTO bidDto) {
        bidService.placeBid(auctionId, bidDto);
        return ResponseEntity.ok("Bid placed successfully");
    }
}