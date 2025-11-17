import { Injectable } from '@angular/core';
import { AuthResponse, JwtPayload, User, UserRole } from '../models/auth.model';

/**
 * Service responsible for storing and retrieving JWT tokens
 * Supports both session storage (default) and local storage (remember me)
 */
@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly EXPIRES_AT_KEY = 'expires_at';
  private readonly REMEMBER_ME_KEY = 'remember_me';

  /**
   * Save authentication tokens
   * @param authResponse Response from login/refresh endpoint
   * @param rememberMe If true, use localStorage instead of sessionStorage
   */
  saveTokens(authResponse: AuthResponse, rememberMe: boolean = false): void {
    const storage = this.getStorage(rememberMe);
    const expiresAt = Date.now() + (authResponse.expiresIn * 1000);

    storage.setItem(this.ACCESS_TOKEN_KEY, authResponse.accessToken);
    storage.setItem(this.REFRESH_TOKEN_KEY, authResponse.refreshToken);
    storage.setItem(this.EXPIRES_AT_KEY, expiresAt.toString());
    storage.setItem(this.REMEMBER_ME_KEY, rememberMe.toString());
  }

  /**
   * Get access token
   * @returns Access token or null if not found
   */
  getAccessToken(): string | null {
    return this.getFromBothStorages(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Get refresh token
   * @returns Refresh token or null if not found
   */
  getRefreshToken(): string | null {
    return this.getFromBothStorages(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get token expiration timestamp
   * @returns Expiration timestamp in milliseconds or null
   */
  getExpiresAt(): number | null {
    const expiresAtStr = this.getFromBothStorages(this.EXPIRES_AT_KEY);
    return expiresAtStr ? parseInt(expiresAtStr, 10) : null;
  }

  /**
   * Check if remember me was enabled
   * @returns True if user chose to remember login
   */
  isRememberMeEnabled(): boolean {
    const rememberMe = this.getFromBothStorages(this.REMEMBER_ME_KEY);
    return rememberMe === 'true';
  }

  /**
   * Check if token is expired or about to expire
   * @param bufferSeconds Buffer time in seconds before actual expiration (default: 60s)
   * @returns True if token is expired or will expire within buffer time
   */
  isTokenExpired(bufferSeconds: number = 60): boolean {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) {
      return true;
    }

    const bufferMs = bufferSeconds * 1000;
    return Date.now() >= (expiresAt - bufferMs);
  }

  /**
   * Get time until token expiration in milliseconds
   * @returns Milliseconds until expiration, or 0 if already expired/not found
   */
  getTimeUntilExpiration(): number {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) {
      return 0;
    }

    const timeLeft = expiresAt - Date.now();
    return Math.max(0, timeLeft);
  }

  /**
   * Decode JWT token payload
   * @param token JWT token string
   * @returns Decoded payload or null if invalid
   */
  decodeToken(token: string): JwtPayload | null {
    try {
      const payload = token.split('.')[1];
      const decoded = atob(payload);
      return JSON.parse(decoded) as JwtPayload;
    } catch (error) {
      return null;
    }
  }

  /**
   * Get current user information from access token
   * @returns User object or null if not authenticated
   */
  getCurrentUser(): User | null {
    const token = this.getAccessToken();
    if (!token) {
      return null;
    }

    const payload = this.decodeToken(token);
    if (!payload) {
      return null;
    }

    const roles = payload.authorities.map(auth => auth.authority as UserRole);
    const isAdmin = roles.includes(UserRole.ADMIN);

    return {
      username: payload.sub,
      roles,
      isAdmin
    };
  }

  /**
   * Clear all authentication data
   */
  clear(): void {
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(this.EXPIRES_AT_KEY);
    sessionStorage.removeItem(this.REMEMBER_ME_KEY);

    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.EXPIRES_AT_KEY);
    localStorage.removeItem(this.REMEMBER_ME_KEY);
  }

  /**
   * Get storage based on remember me preference
   * @param rememberMe If true, return localStorage, otherwise sessionStorage
   * @returns Storage object
   */
  private getStorage(rememberMe: boolean): Storage {
    return rememberMe ? localStorage : sessionStorage;
  }

  /**
   * Try to get value from both session and local storage
   * @param key Storage key
   * @returns Value from storage or null
   */
  private getFromBothStorages(key: string): string | null {
    return sessionStorage.getItem(key) || localStorage.getItem(key);
  }
}
