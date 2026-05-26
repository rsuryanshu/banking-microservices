package com.banking.auth_service.service;

import com.banking.auth_service.entity.RefreshToken;
import com.banking.auth_service.entity.User;
import com.banking.auth_service.repository.RefreshTokenRepository;
import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionType;
import com.banking.common_config.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtUtil.generateRefreshToken(user.getUsername()));
        refreshToken.setExpiresAt(Instant.now().plusMillis(604800000));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BankingException(BankingExceptionType.UNAUTHORIZED, "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // reuse detected — revoke all sessions
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw new BankingException(BankingExceptionType.UNAUTHORIZED,
                    "Refresh token reuse detected. All sessions invalidated. Please login again.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BankingException(BankingExceptionType.UNAUTHORIZED,
                    "Refresh token expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BankingException(
                        BankingExceptionType.UNAUTHORIZED, "Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
