// src/main/java/com/example/online_auction/config/WebSocketConfig.java
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
        config.setPort(9090);

        SocketIOServer server = new SocketIOServer(config);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        return server;
    }
}