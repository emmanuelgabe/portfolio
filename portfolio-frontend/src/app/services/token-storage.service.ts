import { inject, Injectable } from '@angular/core';
import { AccessTokenResponse, JwtPayload, User, UserRole } from '../models/auth.model';
import { LoggerService } from './logger.service';

/**
 * Service responsible for storing and retrieving JWT access tokens.
 *
 * Security architecture:
 * - Access token: stored in memory only (this service)
 * - Refresh token: stored in HttpOnly cookie (managed by backend)
 *
 * This hybrid approach protects against XSS attacks:
 * - Access token in memory is lost on page refresh but has short lifetime (15 min)
 * - Refresh token in HttpOnly cookie cannot be accessed by JavaScript
 * - Session can be restored via /api/auth/refresh which reads the cookie
 */
@Injectable({
  providedIn: 'root',
})
export class TokenStorageService {
  private readonly logger = inject(LoggerService);

  // In-memory storage only - NOT localStorage/sessionStorage
  private accessToken: string | null = null;
  private expiresAt: number | null = null;
  private username: string | null = null;
  private role: string | null = null;

  /**
   * Save access token from authentication response.
   * Only the access token is stored in memory.
   * Refresh token is handled via HttpOnly cookie by the backend.
   *
   * @param response Access token response from login/refresh endpoint
   */
  saveTokens(response: AccessTokenResponse): void {
    this.logger.debug('[AUTH] Saving access token', {
      username: response.username,
      role: response.role,
    });
    this.accessToken = response.accessToken;
    this.expiresAt = Date.now() + response.expiresIn;
    this.username = response.username;
    this.role = response.role;
    this.logger.info('[AUTH] Access token saved', {
      username: response.username,
      expiresIn: response.expiresIn,
    });
  }

  /**
   * Get access token from memory.
   * @returns Access token or null if not authenticated
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * Get token expiration timestamp.
   * @returns Expiration timestamp in milliseconds or null
   */
  getExpiresAt(): number | null {
    return this.expiresAt;
  }

  /**
   * Check if token is expired or about to expire.
   * @param bufferSeconds Buffer time in seconds before actual expiration (default: 60s)
   * @returns True if token is expired or will expire within buffer time
   */
  isTokenExpired(bufferSeconds: number = 60): boolean {
    if (!this.expiresAt) {
      return true;
    }

    const bufferMs = bufferSeconds * 1000;
    return Date.now() >= this.expiresAt - bufferMs;
  }

  /**
   * Get time until token expiration in milliseconds.
   * @returns Milliseconds until expiration, or 0 if already expired/not found
   */
  getTimeUntilExpiration(): number {
    if (!this.expiresAt) {
      return 0;
    }

    const timeLeft = this.expiresAt - Date.now();
    return Math.max(0, timeLeft);
  }

  /**
   * Check if user has a valid (non-expired) access token.
   * @returns True if authenticated with valid token
   */
  hasValidToken(): boolean {
    return this.accessToken !== null && !this.isTokenExpired();
  }

  /**
   * Decode JWT token payload.
   * @param token JWT token string
   * @returns Decoded payload or null if invalid
   */
  decodeToken(token: string): JwtPayload | null {
    try {
      const payload = token.split('.')[1];
      const decoded = atob(payload);
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }

  /**
   * Get current user information from stored data.
   * @returns User object or null if not authenticated
   */
  getCurrentUser(): User | null {
    if (!this.accessToken || !this.username) {
      return null;
    }

    const roles: UserRole[] = [];
    if (this.role === 'ROLE_ADMIN' || this.role === 'ADMIN') {
      roles.push(UserRole.ADMIN);
    }

    return {
      username: this.username,
      roles,
      isAdmin: roles.includes(UserRole.ADMIN),
    };
  }

  /**
   * Clear all authentication data from memory.
   * Called on logout or when session expires.
   */
  clear(): void {
    this.logger.debug('[AUTH] Clearing authentication data', { username: this.username });
    this.accessToken = null;
    this.expiresAt = null;
    this.username = null;
    this.role = null;
    this.logger.info('[AUTH] Authentication data cleared');
  }

  /**
   * Check if there might be a valid session (refresh token cookie).
   * Since we can't read HttpOnly cookies, we return true to trigger
   * a refresh attempt if no access token is in memory.
   *
   * @returns True if a session restore should be attempted
   */
  shouldAttemptSessionRestore(): boolean {
    const shouldRestore = this.accessToken === null;
    if (shouldRestore) {
      this.logger.debug('[AUTH] No access token in memory, session restore recommended');
    }
    return shouldRestore;
  }
}
