package com.example.online_auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static images từ classpath
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
                
        // Tạm thời giữ uploads để test
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
                
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
    }
}