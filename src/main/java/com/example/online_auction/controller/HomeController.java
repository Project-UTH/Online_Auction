package com.example.online_auction.controller;

import com.example.online_auction.dto.JwtResponseDTO;
import com.example.online_auction.dto.LoginDTO;
import com.example.online_auction.dto.RegisterDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.User;
import com.example.online_auction.service.AuctionService;
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

import java.util.List;

@Controller
@RequestMapping("/")
public class HomeController {
    @Autowired
    private AuthService authService;
    @Autowired
    private AuctionService auctionService;

    // Hiển thị trang chủ
    @GetMapping
    public String home(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") != null) {
            String displayName = (String) session.getAttribute("displayName");
            model.addAttribute("displayName", displayName != null ? displayName : "Người dùng");
            model.addAttribute("isAdmin", "ADMIN".equals(session.getAttribute("role"))); // Thêm kiểm tra role
        }
        return "home/index"; // Trả về index.html (không hiển thị danh sách auctions)
    }

    // Hiển thị trang danh sách phiên đấu giá
    @GetMapping("/auction-list")
    public String auctionList(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null || !"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/";
        }
        List<Auction> auctions = auctionService.getAllAuctions();
        model.addAttribute("auctions", auctions);
        return "admin/auction-list"; // Trang mới cho danh sách phiên đấu giá
    }

    // Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "home/login"; // Trả về login.html
    }

    // Xử lý đăng nhập - THÊM USERNAME VÀ TOKEN VÀO localStorage
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
            session.setAttribute("username", loginDTO.getUsername()); // Lưu username vào session
            User user = authService.findUserByUsername(loginDTO.getUsername());
            session.setAttribute("displayName", user.getDisplayName());

            // Thêm script để lưu vào localStorage qua redirect
            String script = String.format(
                "<script>localStorage.setItem('jwtToken', '%s'); localStorage.setItem('username', '%s');</script>",
                response.getToken().replace("'", "\\'"), loginDTO.getUsername().replace("'", "\\'")
            );
            redirectAttributes.addFlashAttribute("script", script);

            System.out.println("Login successful, redirecting to /");
            return "redirect:/"; // Chuyển về trang chủ sau khi đăng nhập
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            model.addAttribute("loginDTO", loginDTO);
            model.addAttribute("error", "Đăng nhập thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
            return "home/login"; // Quay lại trang login nếu lỗi
        }
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "home/register"; // Trả về register.html
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
            return "redirect:/login"; // Chuyển sang trang login sau khi đăng ký
        } catch (Exception e) {
            System.out.println("Register error: " + e.getMessage());
            model.addAttribute("registerDTO", registerDTO);
            model.addAttribute("error", "Đăng ký thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
            return "home/register"; // Quay lại trang register nếu lỗi
        }
    }

    // Đăng xuất
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        // Xóa token và username khỏi localStorage khi logout
        String script = "<script>localStorage.removeItem('jwtToken'); localStorage.removeItem('username');</script>";
        RedirectAttributes redirectAttrs = RedirectAttributesHolder.getCurrentRedirectAttributes();
        if (redirectAttrs != null) {
            redirectAttrs.addFlashAttribute("script", script);
        }
        return "redirect:/login";
    }

    // Holder class để truy cập RedirectAttributes (nếu cần)
    private static class RedirectAttributesHolder {
        private static ThreadLocal<RedirectAttributes> redirectAttributes = new ThreadLocal<>();

        public static void setRedirectAttributes(RedirectAttributes attrs) {
            redirectAttributes.set(attrs);
        }

        public static RedirectAttributes getCurrentRedirectAttributes() {
            return redirectAttributes.get();
        }

        public static void clear() {
            redirectAttributes.remove();
        }
    }
}