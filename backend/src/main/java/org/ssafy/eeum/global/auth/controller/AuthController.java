package org.ssafy.eeum.global.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.ssafy.eeum.global.auth.dto.AuthDTOs.*;
import org.ssafy.eeum.global.auth.jwt.JwtProvider;
import org.ssafy.eeum.global.auth.model.CustomUserDetails;
import org.ssafy.eeum.global.common.response.RestApiResponse;
import org.ssafy.eeum.global.error.exception.CustomException;
import org.ssafy.eeum.global.error.model.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "Auth", description = "사용자 인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Operation(summary = "CustomUserDetails 확인", description = "토큰을 통해 해석된 CustomUserDetails 정보를 반환합니다.")
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testCustomUserDetails(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Test API Request - Anonymous User");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized: Token missing or invalid"));
        }

        log.info("Test API Request - UserID: {}", userDetails.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userDetails.getId());
        userInfo.put("email", userDetails.getEmail());
        userInfo.put("role", userDetails.getRole());
        return ResponseEntity.ok(userInfo);
    }

    @Operation(summary = "로그아웃", description = "Redis에서 리프레시 토큰을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<RestApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "인증되지 않은 사용자입니다.");
        }

        Integer userId = userDetails.getId();
        String redisKey = "RT:" + userId;

        // Redis에서 리프레시 토큰 삭제
        Boolean deleted = redisTemplate.delete(redisKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("User logged out successfully: userId={}", userId);
        } else {
            log.warn("Refresh token not found in Redis: userId={}", userId);
        }

        return ResponseEntity.ok(RestApiResponse.success("로그아웃 되었습니다."));
    }

    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다. 리프레시 토큰도 함께 갱신됩니다.")
    @PostMapping("/refresh")
    public ResponseEntity<RestApiResponse<TokenResponseDTO>> refreshToken(
            @RequestBody TokenRefreshRequestDTO request) {

        String refreshToken = request.getRefreshToken();

        // 1. 리프레시 토큰 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. 토큰에서 사용자 정보 추출
        org.springframework.security.core.Authentication auth = jwtProvider.getAuthentication(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Integer userId = userDetails.getId();
        String email = userDetails.getEmail();
        String role = userDetails.getRole();

        // 3. Redis에 저장된 리프레시 토큰과 비교
        String redisKey = "RT:" + userId;
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (storedRefreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "로그아웃된 사용자입니다.");
        }

        if (!storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "리프레시 토큰이 일치하지 않습니다.");
        }

        // 4. 새로운 액세스 토큰 및 리프레시 토큰 생성
        String newAccessToken = jwtProvider.createAccessToken(userId, email, role);
        String newRefreshToken = jwtProvider.createRefreshToken(userId, email, role);

        // 5. Redis에 새로운 리프레시 토큰 저장 (기존 토큰 덮어쓰기)
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, 14, TimeUnit.DAYS);

        log.info("Token refreshed successfully: userId={}", userId);

        TokenResponseDTO response = TokenResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

        return ResponseEntity.ok(RestApiResponse.success(HttpStatus.OK, "토큰이 갱신되었습니다.", response));
    }
}
