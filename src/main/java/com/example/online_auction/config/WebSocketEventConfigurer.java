// src/main/java/com/example/online_auction/config/WebSocketEventConfigurer.java (Class mới)
package com.example.online_auction.config;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.example.online_auction.dto.BidDTO;
import com.example.online_auction.service.BidService;
import com.example.online_auction.service.CustomUserDetailsService;
import com.example.online_auction.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;

@Configuration
public class WebSocketEventConfigurer {

    @Autowired
    private SocketIOServer socketIOServer;

    @Autowired
    private BidService bidService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostConstruct
    public void configure() {
        // Xử lý kết nối với authentication
        socketIOServer.addConnectListener((client) -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            String auctionId = client.getHandshakeData().getSingleUrlParam("auctionId");
            if (token == null || auctionId == null) {
                client.disconnect();
                return;
            }
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.validateToken(token, userDetails)) {
                        client.set("username", username);
                        client.joinRoom(auctionId);
                        bidService.joinAuction(auctionId, username);
                        System.out.println("User " + username + " joined auction " + auctionId);
                        return;
                    }
                }
                client.disconnect();
            } catch (Exception e) {
                client.disconnect();
            }
        });

        // Xử lý ngắt kết nối
        socketIOServer.addDisconnectListener((client) -> {
            String auctionId = client.getHandshakeData().getSingleUrlParam("auctionId");
            String username = (String) client.get("username");
            if (auctionId != null && username != null) {
                bidService.leaveAuction(auctionId, username);
            }
            System.out.println("Client disconnected: " + client.getSessionId());
        });

        // Đăng ký event listener cho bid
        socketIOServer.addEventListener("placeBid", BidDTO.class, (SocketIOClient client, BidDTO bidDto, AckRequest ackRequest) -> {
            String auctionId = client.getHandshakeData().getSingleUrlParam("auctionId");
            String username = (String) client.get("username");
            if (auctionId != null && username != null) {
                bidDto.setUsername(username);
                bidService.placeBid(auctionId, bidDto);
            }
        });
    }
}