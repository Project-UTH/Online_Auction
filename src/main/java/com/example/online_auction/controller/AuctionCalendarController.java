package com.example.online_auction.controller;

import com.example.online_auction.entity.Auction;
import com.example.online_auction.service.AuctionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/auction-calendar")
public class AuctionCalendarController {

    @Autowired
    private AuctionService auctionService;

    @GetMapping
    public String showAuctionCalendar(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        model.addAttribute("isAdmin", "ADMIN".equals(role));
        List<Auction> auctions = auctionService.getAllAuctions();
        model.addAttribute("auctions", auctions);
        return "auction-calendar"; // Tạo file auction-calendar.html
    }
}