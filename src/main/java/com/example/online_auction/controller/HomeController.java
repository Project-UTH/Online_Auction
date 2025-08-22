package com.example.online_auction.controller;

import com.example.online_auction.dto.JwtResponseDTO;
import com.example.online_auction.dto.LoginDTO;
import com.example.online_auction.dto.RegisterDTO;
import com.example.online_auction.entity.User;
import com.example.online_auction.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private AuthService authService;

    // Hiển thị trang chủ
    @GetMapping
    public String home(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") != null) {
            String displayName = (String) session.getAttribute("displayName");
            model.addAttribute("displayName", displayName != null ? displayName : "Người dùng");
        }
        // Sau này thêm danh sách phiên đấu giá từ AuctionService
        return "home/index";  // Trả về index.html
    }

    // Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "home/login";  // Trả về login.html
    }

    // Xử lý đăng nhập
    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute LoginDTO loginDTO, BindingResult bindingResult, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        System.out.println("Login attempt for: " + loginDTO.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            return "home/login";
        }
        try {
            JwtResponseDTO response = authService.login(loginDTO);
            session.setAttribute("jwtToken", response.getToken());
            session.setAttribute("role", response.getRole());
            User user = authService.findUserByUsername(loginDTO.getUsername());
            session.setAttribute("displayName", user.getDisplayName());
            System.out.println("Login successful, redirecting to /");
            return "redirect:/";  // Chuyển về trang chủ sau khi đăng nhập
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Đăng nhập thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
            return "home/login";  // Quay lại trang login nếu lỗi
        }
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "home/register";  // Trả về register.html
    }

    // Xử lý đăng ký
    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute RegisterDTO registerDTO, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        System.out.println("Register attempt for: " + registerDTO.getUsername());
        if (bindingResult.hasErrors()) {
            model.addAttribute("registerDTO", registerDTO);
            model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            return "home/register";
        }
        try {
            authService.register(registerDTO);
            System.out.println("Register successful, redirecting to /login");
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";  // Chuyển sang trang login sau khi đăng ký
        } catch (Exception e) {
            System.out.println("Register error: " + e.getMessage());
            model.addAttribute("registerDTO", registerDTO);
            model.addAttribute("error", "Đăng ký thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
            return "home/register";  // Quay lại trang register nếu lỗi
        }
    }

    // Đăng xuất
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}