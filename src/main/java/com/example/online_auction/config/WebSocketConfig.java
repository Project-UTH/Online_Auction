package com.example.online_auction.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(9090); // Sử dụng cổng khác (9090) để tránh xung đột với Tomcat (8080)
        SocketIOServer server = new SocketIOServer(config);
        
        // Khởi động server khi bean được tạo
        server.start();
        
        // Đăng ký shutdown hook để dừng server khi ứng dụng dừng
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        return server;
    }
}