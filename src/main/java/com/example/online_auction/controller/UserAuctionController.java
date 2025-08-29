package com.example.online_auction.controller;

import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.Bid;
import com.example.online_auction.entity.Product;
import com.example.online_auction.service.AuctionRoomService;
import com.example.online_auction.service.AuctionService;
import com.example.online_auction.service.BidService;
import com.example.online_auction.repository.BidRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.corundumstudio.socketio.SocketIOServer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserAuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRoomService auctionRoomService;

    @Autowired
    private BidService bidService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private SocketIOServer socketIOServer;

    @GetMapping("/auction-room")
    public String getUserAuctionList(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token found, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            System.out.println("User role is ADMIN, redirecting to admin page");
            return "redirect:/admin/auction-list";
        }
        List<Auction> auctions = auctionService.getAllAuctions().stream()
                .filter(auction -> auction.getStatus() == Auction.Status.ACTIVE || auction.getStatus() == Auction.Status.PENDING || auction.getStatus() == Auction.Status.COMPLETED)
                .collect(Collectors.toList());
        model.addAttribute("auctions", auctions);
        return "user/auction-room";
    }

    @GetMapping("/john-room/{id}")
    public String getAuctionRoomDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token found, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            System.out.println("User role is ADMIN, redirecting to admin view");
            return "redirect:/admin/view-auction/" + id;
        }
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            model.addAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/user/auction-room";
        }
        auction = auctionService.updateAuctionStatus(auction);

        // Lấy giá thầu cao nhất, người đặt giá, lịch sử đấu giá, số lượt đấu giá, và số người tham gia
        for (Product product : auction.getProducts()) {
            Bid highestBid = bidRepository.findTopByProductOrderByAmountDesc(product);
            if (highestBid != null) {
                product.setCurrentPrice(highestBid.getAmount());
                product.setLastBidder(highestBid.getUser().getUsername());
            } else {
                product.setCurrentPrice(product.getStartingPrice());
                product.setLastBidder(null);
            }
            // Lấy lịch sử đấu giá từ database
            List<Bid> bidHistory = bidRepository.findByProductOrderByTimestampDesc(product);
            product.setBidHistory(bidHistory);
            // Cập nhật số lượt đấu giá và người tham gia
            product.setBidCount((long) bidHistory.size());
            product.setUniqueBidders(bidHistory.stream()
                    .map(bid -> bid.getUser().getUsername())
                    .distinct()
                    .count());
        }

        System.out.println("Auction ID: " + auction.getId() + ", Status: " + auction.getStatus());
        System.out.println("Number of products: " + (auction.getProducts() != null ? auction.getProducts().size() : 0));
        auction.getProducts().forEach(p -> System.out.println("Product: " + p.getName() + ", Status: " + p.getStatus() + 
                ", Current Price: " + p.getCurrentPrice() + ", Last Bidder: " + p.getLastBidder() + 
                ", Bid History Size: " + (p.getBidHistory() != null ? p.getBidHistory().size() : 0) +
                ", Bid Count: " + p.getBidCount() + ", Unique Bidders: " + p.getUniqueBidders()));
        
        String username = (String) session.getAttribute("username");
        if (auction.getStatus() == Auction.Status.ACTIVE && username != null) {
            auctionRoomService.joinAuctionRoom(String.valueOf(id), username);
            Set<String> participants = auctionRoomService.getParticipants(String.valueOf(id));
            socketIOServer.getRoomOperations(String.valueOf(id)).sendEvent("participantUpdate",
                    new BidService.ParticipantUpdateMessage(String.valueOf(id), new ArrayList<>(participants)));
            List<BidService.ProductWithCurrentPrice> initialPrices = bidService.getInitialAuctionDetails(String.valueOf(id));
            socketIOServer.getRoomOperations(String.valueOf(id)).sendEvent("initialAuctionDetails", initialPrices);
        }
        model.addAttribute("participants", auctionRoomService.getParticipants(String.valueOf(id)));
        model.addAttribute("auction", auction);

        if (auction.getStatus() == Auction.Status.ACTIVE) {
            LocalDateTime endTime = auction.getAuctionDate().atTime(auction.getEndTime());
            try {
                socketIOServer.getRoomOperations(String.valueOf(id)).sendEvent("auctionDetails",
                        new BidService.AuctionDetailsMessage(endTime.toString()));
                System.out.println("Sent auctionDetails with endTime: " + endTime);
            } catch (Exception e) {
                System.err.println("Failed to send auctionDetails: " + e.getMessage());
            }
        }

        return "user/john-room";
    }
}