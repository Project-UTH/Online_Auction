package com.example.online_auction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OnlineAuctionApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(OnlineAuctionApplication.class, args);
	}

}