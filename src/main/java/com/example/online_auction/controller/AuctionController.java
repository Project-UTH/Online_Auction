package com.example.online_auction.controller;

import com.example.online_auction.dto.AuctionCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.User;
import com.example.online_auction.service.AuctionService;
import com.example.online_auction.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AuctionController {
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private AuthService authService;

    // Trang danh sách phiên đấu giá
    @GetMapping("/auction-list")
    public String getAuctionList(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token found, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            System.out.println("User role: " + role + ", not ADMIN, redirecting to home");
            return "redirect:/";
        }
        List<Auction> auctions = auctionService.getAllAuctions();
        model.addAttribute("auctions", auctions);
        return "auction-list";
    }

    // Trang tạo auction mới
    @GetMapping("/create-auction")
    public String createAuctionForm(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token found, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            System.out.println("User role: " + role + ", not ADMIN, redirecting to home");
            return "redirect:/";
        }
        model.addAttribute("auctionDTO", new AuctionCreateDTO());
        return "admin/create-auction";
    }

    // Submit tạo auction mới
    @PostMapping("/create-auction")
    public String createAuction(
            @ModelAttribute @Valid AuctionCreateDTO auctionDTO,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate auctionDate,
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        System.out.println("Processing POST /admin/create-auction with auctionDTO: " +
                "name=" + auctionDTO.getName() +
                ", auctionDate=" + auctionDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime);
        if (auctionDate == null || startTime == null || endTime == null) {
            System.out.println("Missing required fields: auctionDate, startTime, or endTime");
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", "Ngày và giờ không được để trống.");
            return "admin/create-auction";
        }
        if (bindingResult.hasErrors()) {
            System.out.println("Validation errors: " + bindingResult.getAllErrors());
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; ")));
            return "admin/create-auction";
        }
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                System.out.println("No username in session, redirecting to login");
                return "redirect:/login";
            }
            User admin = authService.findUserByUsername(username);
            if (admin == null) {
                System.out.println("User not found for username: " + username);
                model.addAttribute("error", "Người dùng không tồn tại.");
                return "admin/create-auction";
            }
            auctionDTO.setAuctionDate(auctionDate);
            auctionDTO.setStartTime(startTime);
            auctionDTO.setEndTime(endTime);
            Auction auction = auctionService.createAuction(auctionDTO, admin);
            System.out.println("Create auction successful, ID: " + auction.getId() + ", Status: " + auction.getStatus() + ", Date: " + auction.getAuctionDate());
            redirectAttributes.addFlashAttribute("success", "Tạo phiên đấu giá thành công!");
            return "redirect:/admin/edit-auction/" + auction.getId();
        } catch (IllegalArgumentException e) {
            System.out.println("Create auction error: " + e.getMessage());
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", e.getMessage());
            return "admin/create-auction";
        } catch (Exception e) {
            System.out.println("Create auction error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", "Tạo phiên đấu giá thất bại: " + e.getMessage());
            return "admin/create-auction";
        }
    }

    // Trang chỉnh sửa auction
    @GetMapping("/edit-auction/{id}")
    public String editAuctionForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            System.out.println("User role: " + role + ", not ADMIN, redirecting to home");
            return "redirect:/";
        }
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            System.out.println("Auction ID " + id + " not found");
            redirectAttributes.addFlashAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/auction-list";
        }
        auction = auctionService.updateAuctionStatus(auction);
        System.out.println("Attempting to edit auction ID: " + id + ", Status: " + auction.getStatus());
        if (!Auction.Status.PENDING.equals(auction.getStatus())) {
            System.out.println("Auction status is not PENDING: " + auction.getStatus());
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể chỉnh sửa phiên đấu giá có trạng thái PENDING.");
            return "redirect:/auction-list";
        }
        model.addAttribute("auction", auction);
        model.addAttribute("products", auction.getProducts());
        return "admin/edit-auction";
    }

    // Xử lý nút "Hoàn thành" cho auction
    @PostMapping("/complete-auction/{id}")
    public String completeAuction(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token, redirecting to login");
            return "redirect:/login";
        }
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            System.out.println("User role: " + role + ", not ADMIN, redirecting to home");
            return "redirect:/";
        }
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            System.out.println("Auction ID " + id + " not found");
            redirectAttributes.addFlashAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/auction-list";
        }
        auction = auctionService.updateAuctionStatus(auction);
        System.out.println("Completing auction ID: " + id + ", Status: " + auction.getStatus());
        if (!Auction.Status.PENDING.equals(auction.getStatus())) {
            System.out.println("Auction status is not PENDING: " + auction.getStatus());
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể hoàn thành phiên đấu giá có trạng thái PENDING.");
            return "redirect:/auction-list";
        }
        try {
            if (auction.getProducts() == null || auction.getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Phiên đấu giá phải có ít nhất một sản phẩm.");
                return "redirect:/admin/edit-auction/" + id;
            }
            auction.setStatus(Auction.Status.ACTIVE);
            auctionService.saveAuction(auction);
            System.out.println("Auction completed, new status: " + auction.getStatus());
            redirectAttributes.addFlashAttribute("success", "Phiên đấu giá đã được hoàn thành và kích hoạt!");
            return "redirect:/auction-list";
        } catch (Exception e) {
            System.out.println("Complete auction error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Hoàn thành phiên đấu giá thất bại: " + e.getMessage());
            return "redirect:/admin/edit-auction/" + id;
        }
    }

    // API để lấy danh sách ngày có phiên đấu giá
    @GetMapping("/api/auctions/dates")
    public ResponseEntity<List<String>> getAuctionDates() {
        List<LocalDate> dates = auctionService.getAuctionDates();
        System.out.println("Auction dates retrieved: " + dates); // Debug log
        if (dates == null || dates.isEmpty()) {
            System.out.println("No auction dates found in database");
        }
        return ResponseEntity.ok(dates.stream()
                .map(LocalDate::toString)
                .toList());
    }

    // API để lấy danh sách phiên đấu giá theo ngày
    @GetMapping("/api/auctions/by-date")
    public ResponseEntity<List<Auction>> getAuctionsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            System.out.println("Fetching auctions for date: " + date); // Debug log
            List<Auction> auctions = auctionService.getAuctionsByDate(date);
            if (auctions == null || auctions.isEmpty()) {
                System.out.println("No auctions found for date: " + date);
                return ResponseEntity.ok(List.of()); // Trả về danh sách rỗng
            }
            System.out.println("Auctions found: " + auctions.size() + " - Details: " + auctions); // Debug log
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            System.err.println("Error in getAuctionsByDate: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of()); // Trả về danh sách rỗng khi lỗi
        }
    }

    // API để lấy tất cả phiên đấu giá
    @GetMapping("/api/auctions")
    public ResponseEntity<List<Auction>> getAllAuctions() {
        try {
            List<Auction> auctions = auctionService.getAllAuctions();
            System.out.println("All auctions retrieved: " + auctions); // Debug log
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            System.err.println("Error in getAllAuctions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    // Trang xem chi tiết phiên đấu giá
    @GetMapping("view-auction/{id}")
    public String viewAuction(@PathVariable Long id, Model model, HttpSession session) {
        System.out.println("Accessing /view-auction/" + id); // Debug log
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token, redirecting to login");
            return "redirect:/login";
        }
        Auction auction = auctionService.getAuctionById(id);
        if (auction == null) {
            model.addAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/calendar";
        }
        auction = auctionService.updateAuctionStatus(auction);
        model.addAttribute("auction", auction);
        return "admin/view-auction";
    }
}