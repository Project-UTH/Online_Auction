package com.example.online_auction.controller;

import com.example.online_auction.entity.Auction;
import com.example.online_auction.service.AuctionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AuctionDetailController {

    @Autowired
    private AuctionService auctionService;

    @GetMapping("/auction/detail/{id}")
    public String showAuctionDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            return "redirect:/login";
        }
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            model.addAttribute("error", "Phiên đấu giá không tồn tại.");
            return "error";
        }
        model.addAttribute("auction", auction);
        model.addAttribute("isAdmin", "ADMIN".equals(session.getAttribute("role")));
        return "auction-detail"; // Tạo template auction-detail.html
    }
}