// src/main/java/com/example/online_auction/service/AuctionRoomService.java (File mới, tách logic room từ BidService)
package com.example.online_auction.service;

import com.example.online_auction.entity.Auction;
import com.example.online_auction.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuctionRoomService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuctionRepository auctionRepository;

    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String PARTICIPANTS_SUFFIX = ":participants";

    public void joinAuctionRoom(String auctionId, String username) {
        Auction auction = auctionRepository.findById(Long.parseLong(auctionId))
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Kiểm tra auction có cho phép join (ví dụ active)
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            throw new RuntimeException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.");
        }

        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants == null) participants = new HashSet<>();
        participants.add(username);
        redisTemplate.opsForValue().set(key, participants);
    }

    public void leaveAuctionRoom(String auctionId, String username) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        if (participants != null) {
            participants.remove(username);
            redisTemplate.opsForValue().set(key, participants);
        }
    }

    public Set<String> getParticipants(String auctionId) {
        String key = AUCTION_KEY_PREFIX + auctionId + PARTICIPANTS_SUFFIX;
        @SuppressWarnings("unchecked")
        Set<String> participants = (Set<String>) redisTemplate.opsForValue().get(key);
        return participants != null ? participants : new HashSet<>();
    }
}