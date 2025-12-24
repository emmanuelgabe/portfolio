package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Access token response DTO.
 * Contains only access token and user information.
 * Refresh token is sent via HttpOnly cookie for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessTokenResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresIn;
    private String username;
    private String role;
}
