package com.example.online_auction.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    public void setUserSessionAttributes(HttpSession session, String token, String username, String role) {
        if (session != null) {
            session.setAttribute("jwtToken", token);
            session.setAttribute("username", username);
            session.setAttribute("role", role);
        }
    }
    
    public void clearSessionAttributes(HttpSession session) {
        if (session != null) {
            session.removeAttribute("jwtToken");
            session.removeAttribute("username");
            session.removeAttribute("role");
        }
    }
}