package com.example.UserSignupJwtRole.user.dto;


public record AuthResponse(
        String accessToken,
        String tokenType
) {
}
