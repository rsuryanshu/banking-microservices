package com.banking.auth_service.controller;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginDTO;
import com.banking.auth_service.dto.RegisterDTO;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.User;
import com.banking.auth_service.service.UserService;
import com.banking.common_config.jwt.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
       return new ResponseEntity<>("Register Successfully", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));
        User user = userService.getUserByUsername(loginDTO.getUsername());
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtUtil.generateToken(user.getUsername(), roles);
        return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
    }

}
