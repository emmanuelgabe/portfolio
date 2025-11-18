/**
 * Authentication models and DTOs
 * Matches backend AuthController DTOs
 */

/**
 * Login request payload
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Authentication response from backend
 * Contains both access token and refresh token
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/**
 * Token refresh request payload
 */
export interface TokenRefreshRequest {
  refreshToken: string;
}

/**
 * User role enum matching backend UserRole
 */
export enum UserRole {
  ADMIN = 'ROLE_ADMIN',
  GUEST = 'ROLE_GUEST'
}

/**
 * Decoded JWT token payload
 */
export interface JwtPayload {
  sub: string;
  authorities: Array<{ authority: string }>;
  type: string;
  iss: string;
  aud: string;
  iat: number;
  exp: number;
}

/**
 * User information extracted from JWT
 */
export interface User {
  username: string;
  roles: UserRole[];
  isAdmin: boolean;
}
