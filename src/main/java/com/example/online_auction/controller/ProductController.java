package com.example.online_auction.controller;

import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.service.AuctionService;
import com.example.online_auction.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AuctionService auctionService;

    // Trang thêm sản phẩm cho auction cụ thể
    @GetMapping("/add-product/{auctionId}")
    public String addProductForm(@PathVariable Long auctionId, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("jwtToken") == null) {
            System.out.println("No JWT token, redirecting to login");
            return "redirect:/login";
        }

        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            System.out.println("User role: " + role + ", not ADMIN, redirecting to home");
            return "redirect:/";
        }

        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            System.out.println("Auction ID " + auctionId + " not found");
            redirectAttributes.addFlashAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/auction-list";
        }

        System.out.println("Attempting to add product to auction ID: " + auctionId + ", Status: " + auction.getStatus());
        if (!Auction.Status.PENDING.equals(auction.getStatus())) {
            System.out.println("Auction status is not PENDING: " + auction.getStatus());
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể thêm sản phẩm vào phiên đấu giá có trạng thái PENDING.");
            return "redirect:/auction-list";
        }

        model.addAttribute("productDTO", new ProductCreateDTO());
        model.addAttribute("auctionId", auctionId);
        return "admin/add-product";
    }

    // Submit thêm sản phẩm
    @PostMapping("/add-product/{auctionId}")
    public String addProduct(
            @PathVariable Long auctionId,
            @ModelAttribute @Valid ProductCreateDTO productDTO,
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            System.out.println("Auction ID " + auctionId + " not found");
            redirectAttributes.addFlashAttribute("error", "Phiên đấu giá không tồn tại.");
            return "redirect:/auction-list";
        }

        System.out.println("Adding product to auction ID: " + auctionId + ", Status: " + auction.getStatus());
        if (!Auction.Status.PENDING.equals(auction.getStatus())) {
            System.out.println("Auction status is not PENDING: " + auction.getStatus());
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể thêm sản phẩm vào phiên đấu giá có trạng thái PENDING.");
            return "redirect:/auction-list";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("productDTO", productDTO);
            model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            return "admin/add-product";
        }

        try {
            System.out.println("=== ADD PRODUCT DEBUG ===");
            System.out.println("Product name: " + productDTO.getName());
            System.out.println("Start time: " + productDTO.getStartTime());
            System.out.println("End time: " + productDTO.getEndTime());
            System.out.println("Image file: " + (productDTO.getImageFile() != null ? productDTO.getImageFile().getOriginalFilename() : "null"));

            if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Tên sản phẩm không được để trống.");
                return "admin/add-product";
            }

            if (productDTO.getStartingPrice() == null || productDTO.getStartingPrice() <= 0) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Giá khởi điểm phải lớn hơn 0.");
                return "admin/add-product";
            }

            if (productDTO.getMinimumBidIncrement() == null || productDTO.getMinimumBidIncrement() <= 0) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Tăng giá tối thiểu phải lớn hơn 0.");
                return "admin/add-product";
            }

            if (productDTO.getStartTime() == null || productDTO.getEndTime() == null) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Vui lòng nhập thời gian bắt đầu và kết thúc.");
                return "admin/add-product";
            }

            productDTO.setStartTime(startTime);
            productDTO.setEndTime(endTime);

            String imageUrl = handleImageUpload(productDTO.getImageFile());
            productDTO.setImageUrl(imageUrl);

            System.out.println("Final image URL: " + imageUrl);

            // Gọi ProductService để lưu sản phẩm
            productService.addProductToAuction(auctionId, productDTO);
            System.out.println("Product saved to database, ID: " + auctionId);

            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công!");
            return "redirect:/admin/edit-auction/" + auctionId;
        } catch (Exception e) {
            System.err.println("Add product error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("productDTO", productDTO);
            model.addAttribute("error", "Thêm sản phẩm thất bại: " + e.getMessage());
            return "admin/add-product";
        }
    }

    /**
     * Handle image upload and return image URL
     */
    private String handleImageUpload(MultipartFile imageFile) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            System.out.println("No image uploaded, using default");
            return "/images/default-product.png";
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("File không phải là ảnh hợp lệ");
        }

        String uploadDir = "src/main/resources/static/images/products/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        String originalFilename = imageFile.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.lastIndexOf(".") > 0
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFilename);

        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        String imageUrl = "/images/products/" + uniqueFilename;
        System.out.println("Image uploaded successfully to: " + filePath.toAbsolutePath());
        System.out.println("Image URL: " + imageUrl);

        return imageUrl;
    }
}