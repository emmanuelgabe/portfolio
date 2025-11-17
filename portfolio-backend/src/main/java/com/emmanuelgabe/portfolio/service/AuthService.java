package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.dto.ChangePasswordRequest;
import com.emmanuelgabe.portfolio.dto.LoginRequest;

/**
 * Service interface for Authentication operations
 * Provides business logic for user authentication and token management
 */
public interface AuthService {

    /**
     * Authenticate user with username and password
     * @param loginRequest Login credentials
     * @return Authentication response with tokens
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Refresh access token using refresh token
     * @param refreshToken Refresh token
     * @return New authentication response with fresh tokens
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Logout user by revoking refresh token
     * @param refreshToken Refresh token to revoke
     */
    void logout(String refreshToken);

    /**
     * Change user password
     * @param username Username
     * @param changePasswordRequest Password change request
     */
    void changePassword(String username, ChangePasswordRequest changePasswordRequest);
}
