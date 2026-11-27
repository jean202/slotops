package com.jean325.slotops.auth.application;

import com.jean325.slotops.auth.domain.User;
import com.jean325.slotops.auth.domain.UserRepository;
import com.jean325.slotops.auth.domain.UserStatus;
import com.jean325.slotops.auth.dto.LoginRequest;
import com.jean325.slotops.auth.dto.LoginResponse;
import com.jean325.slotops.auth.dto.SignupRequest;
import com.jean325.slotops.auth.dto.SignupResponse;
import com.jean325.slotops.auth.dto.TokenRefreshRequest;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ApiException(
                "EMAIL_ALREADY_EXISTS",
                "Email already registered",
                HttpStatus.CONFLICT
            );
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name());
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        User savedUser = userRepository.save(user);
        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getName());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ApiException(
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                HttpStatus.UNAUTHORIZED
            ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                HttpStatus.UNAUTHORIZED
            );
        }

        requireActive(user);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            accessToken,
            refreshToken,
            3600000 // 1 hour in milliseconds
        );
    }

    public LoginResponse refreshToken(TokenRefreshRequest request) {
        if (!tokenProvider.isRefreshTokenValid(request.refreshToken())) {
            throw new ApiException(
                "INVALID_REFRESH_TOKEN",
                "Invalid or expired refresh token",
                HttpStatus.UNAUTHORIZED
            );
        }

        Long userId = tokenProvider.extractUserId(request.refreshToken());
        String email = tokenProvider.extractEmail(request.refreshToken());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(
                "USER_NOT_FOUND",
                "User not found",
                HttpStatus.NOT_FOUND
            ));

        requireActive(user);

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            newAccessToken,
            newRefreshToken,
            3600000
        );
    }

    // Checked after the password so the account status is not revealed to callers without credentials.
    // Access tokens already issued stay valid until they expire (the JWT filter does not hit the DB).
    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(
                "ACCOUNT_NOT_ACTIVE",
                "This account is not active",
                HttpStatus.FORBIDDEN
            );
        }
    }
}
