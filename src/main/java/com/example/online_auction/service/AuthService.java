package com.example.online_auction.service;

import com.example.online_auction.dto.JwtResponseDTO;
import com.example.online_auction.dto.LoginDTO;
import com.example.online_auction.dto.RegisterDTO;
import com.example.online_auction.entity.User;
import com.example.online_auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.online_auction.util.JwtUtil;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    public User register(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername()))
            throw new RuntimeException("Username already exists");
        if (userRepository.existsByEmail(registerDTO.getEmail()))
            throw new RuntimeException("Email already exists");

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setFullName(registerDTO.getFullName());
        user.setDisplayName(registerDTO.getDisplayName());
        user.setAddress(registerDTO.getAddress());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setRole(User.Role.USER);
        user.setVerified(false);

        return userRepository.save(user);
    }

    public JwtResponseDTO login(LoginDTO loginDTO) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );
        User user = userRepository.findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new JwtResponseDTO(token, user.getRole().name());  // Sử dụng constructor mới
    }

    public User findUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
    }
}