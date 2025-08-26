package com.example.online_auction.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("localhost");
        config.setPort(9090);
        config.setOrigin("*");
        config.setAllowCustomRequests(true);

        SocketIOServer server = new SocketIOServer(config);
        server.start();
        System.out.println("Socket.IO server started on port 9090");

        // Đăng ký shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        return server;
    }
}
