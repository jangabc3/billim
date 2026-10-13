package com.billim.api.auth;

import com.billim.common.error.ApiException;
import com.billim.common.error.ErrorCode;
import com.billim.config.security.CustomUserDetails;
import com.billim.config.security.JwtTokenProvider;
import com.billim.domain.user.User;
import com.billim.domain.user.UserRole;
import com.billim.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name(), UserRole.USER);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 응답으로 처리한다(계정 존재 여부 노출 방지).
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.isLocked()) {
            throw new ApiException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.recordLoginFailure();
            userRepository.save(user);
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        user.recordLoginSuccess();
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, user.getId(), user.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getRole().name()));
    }
}
