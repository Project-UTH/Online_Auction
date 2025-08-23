package com.example.online_auction.controller;

import com.example.online_auction.dto.AuctionCreateDTO;
import com.example.online_auction.dto.ProductCreateDTO;
import com.example.online_auction.entity.Auction;
import com.example.online_auction.entity.User;
import com.example.online_auction.service.AuctionService;
import com.example.online_auction.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuthService authService;

    // Trang tạo auction
    @GetMapping("/create-auction")
    public String createAuctionForm(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            return "redirect:/login";
        }
        
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/";
        }
        
        Long auctionId = (Long) session.getAttribute("currentAuctionId");
        List<ProductCreateDTO> products = new java.util.ArrayList<>();
        
        if (auctionId != null) {
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction != null) {
                products = auction.getProducts().stream().map(product -> {
                    ProductCreateDTO dto = new ProductCreateDTO();
                    dto.setName(product.getName());
                    dto.setDescription(product.getDescription());
                    dto.setStartingPrice(product.getStartingPrice());
                    dto.setMinimumBidIncrement(product.getMinimumBidIncrement());
                    dto.setImageUrl(product.getImageUrl());
                    dto.setStartTime(product.getStartTime());
                    dto.setEndTime(product.getEndTime());
                    return dto;
                }).collect(Collectors.toList());
                System.out.println("Products loaded in createAuctionForm: " + products.size());
            }
        }
        
        model.addAttribute("auctionDTO", new AuctionCreateDTO());
        model.addAttribute("products", products);
        
        System.out.println("Accessing create-auction page with role: " + role);
        return "admin/create-auction";
    }

    // Submit tạo auction
    @PostMapping("/create-auction")
    public String createAuction(@ModelAttribute AuctionCreateDTO auctionDTO, 
                               BindingResult bindingResult, 
                               HttpSession session, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            return "admin/create-auction";
        }
        
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return "redirect:/login";
            }
            
            User admin = authService.findUserByUsername(username);
            Auction auction = auctionService.createAuction(auctionDTO, admin);
            session.setAttribute("currentAuctionId", auction.getId());
            
            System.out.println("Create auction successful, ID: " + auction.getId());
            redirectAttributes.addFlashAttribute("success", "Tạo phiên đấu giá thành công!");
            return "redirect:/admin/create-auction";
        } catch (Exception e) {
            System.out.println("Create auction error: " + e.getMessage());
            model.addAttribute("auctionDTO", auctionDTO);
            model.addAttribute("error", "Tạo phiên đấu giá thất bại: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
            return "admin/create-auction";
        }
    }

    // Trang add product
    @GetMapping("/add-product")
    public String addProductForm(Model model, HttpSession session) {
        if (session.getAttribute("jwtToken") == null) {
            return "redirect:/login";
        }
        
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/";
        }
        
        model.addAttribute("productDTO", new ProductCreateDTO());
        return "admin/add-product";
    }

    // Submit add product - FIXED VERSION
    @PostMapping("/add-product")
    public String addProduct(@ModelAttribute ProductCreateDTO productDTO, 
                            BindingResult bindingResult, 
                            HttpSession session, 
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Long auctionId = (Long) session.getAttribute("currentAuctionId");
        if (auctionId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng tạo phiên đấu giá trước.");
            return "redirect:/admin/create-auction";
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("productDTO", productDTO);
            model.addAttribute("error", "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
            return "admin/add-product";
        }
        
        try {
            // Log dữ liệu đầu vào
            System.out.println("=== ADD PRODUCT DEBUG ===");
            System.out.println("Product name: " + productDTO.getName());
            System.out.println("Start time: " + productDTO.getStartTime());
            System.out.println("End time: " + productDTO.getEndTime());
            System.out.println("Image file: " + (productDTO.getImageFile() != null ? productDTO.getImageFile().getOriginalFilename() : "null"));

            // Validate required fields
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

            // Validate time fields
            if (productDTO.getStartTime() == null || productDTO.getEndTime() == null) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Vui lòng nhập thời gian bắt đầu và kết thúc.");
                return "admin/add-product";
            }
            
            if (productDTO.getStartTime().isAfter(productDTO.getEndTime())) {
                model.addAttribute("productDTO", productDTO);
                model.addAttribute("error", "Thời gian bắt đầu phải trước thời gian kết thúc.");
                return "admin/add-product";
            }

            // Handle image upload
            String imageUrl = handleImageUpload(productDTO.getImageFile());
            productDTO.setImageUrl(imageUrl);
            
            System.out.println("Final image URL: " + imageUrl);

            // Add product to auction
            auctionService.addProductToAuction(auctionId, productDTO);
            System.out.println("Product added successfully!");

            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công!");
            return "redirect:/admin/create-auction";
            
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

    // Validate file type
    String contentType = imageFile.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new IOException("File không phải là ảnh hợp lệ");
    }

    // Create static/images/products directory
    String uploadDir = "src/main/resources/static/images/products/";
    Path uploadPath = Paths.get(uploadDir);
    if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
        System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
    }

    // Generate unique filename
    String originalFilename = imageFile.getOriginalFilename();
    String fileExtension = "";
    if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
        fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    
    String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + fileExtension;
    Path filePath = uploadPath.resolve(uniqueFilename);

    // Save file
    Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    
    // URL để truy cập ảnh
    String imageUrl = "/images/products/" + uniqueFilename;
    System.out.println("Image uploaded successfully to: " + filePath.toAbsolutePath());
    System.out.println("Image URL: " + imageUrl);
    
    return imageUrl;
}


    // Xử lý nút "Hoàn thành"
    @PostMapping("/complete-auction")
    public String completeAuction(HttpSession session, RedirectAttributes redirectAttributes) {
        Long auctionId = (Long) session.getAttribute("currentAuctionId");
        if (auctionId == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy phiên đấu giá.");
            return "redirect:/admin/create-auction";
        }
        
        try {
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy phiên đấu giá.");
                return "redirect:/admin/create-auction";
            }
            
            if (auction.getProducts() == null || auction.getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Phiên đấu giá phải có ít nhất một sản phẩm.");
                return "redirect:/admin/create-auction";
            }
            
            auction.setStatus(Auction.Status.ACTIVE);
            auctionService.saveAuction(auction);
            session.removeAttribute("currentAuctionId");
            
            redirectAttributes.addFlashAttribute("success", "Phiên đấu giá đã được hoàn thành và kích hoạt!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hoàn thành phiên đấu giá thất bại: " + e.getMessage());
            return "redirect:/admin/create-auction";
        }
    }
}
