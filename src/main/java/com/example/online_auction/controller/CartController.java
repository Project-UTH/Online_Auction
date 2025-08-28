package com.example.online_auction.controller;

import com.example.online_auction.entity.Product;
import com.example.online_auction.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public String viewCart(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            List<Product> wonProducts = productService.getWonProductsByUsername(username);
            model.addAttribute("wonProducts", wonProducts);
        } else {
            model.addAttribute("wonProducts", List.of());
        }
        return "user/cart"; // Maps to src/main/resources/templates/cart.html
    }

    @GetMapping("/api/won-products")
    @ResponseBody
    public List<Product> getWonProductsApi() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            return productService.getWonProductsByUsername(username);
        }
        return List.of();
    }
}