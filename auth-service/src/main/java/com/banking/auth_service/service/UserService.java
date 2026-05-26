package com.banking.auth_service.service;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.RefreshRequest;
import com.banking.auth_service.dto.RegisterDTO;
import com.banking.auth_service.entity.RefreshToken;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.User;
import com.banking.auth_service.repository.RoleRepository;
import com.banking.auth_service.repository.UserRepository;
import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionType;
import com.banking.common_config.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public void register(RegisterDTO registerDTO) {
        Optional<User> byUsername = userRepository.findByUsername(registerDTO.getUsername());
        if (byUsername.isPresent()) {
            throw new BankingException(BankingExceptionType.ALREADY_EXIST, "Username already exists");
        }
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BankingException(
                        BankingExceptionType.ELEMENT_NOT_FOUND, "Role not found"));
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BankingException(
                        BankingExceptionType.USER_NOT_FOUND, "Username not found"));
    }

    public User addRole(String username) {
        Role role = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new BankingException(
                        BankingExceptionType.ELEMENT_NOT_FOUND, "Role not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BankingException(
                        BankingExceptionType.USER_NOT_FOUND, "Username not found"));
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public AuthResponse generateAuthResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), roles);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken.getToken(), 900);
    }

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(
                request.getRefreshToken());
        User user = refreshToken.getUser();
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        return generateAuthResponse(user);
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }
}