import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, timer, Subscription } from 'rxjs';
import { tap, catchError, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import { LoginRequest, AuthResponse, TokenRefreshRequest, User } from '../models/auth.model';

/**
 * Authentication service with JWT token management
 * Handles login, logout, token refresh, and auto-refresh scheduling
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly logger = inject(LoggerService);
  private readonly tokenStorage = inject(TokenStorageService);

  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private readonly REFRESH_BUFFER_SECONDS = 60;

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private refreshTimerSubscription?: Subscription;
  private isRefreshing = false;

  constructor() {
    this.initializeAuthState();
  }

  /**
   * Initialize authentication state on service creation
   * Restores user session if valid token exists
   */
  private initializeAuthState(): void {
    const user = this.tokenStorage.getCurrentUser();
    if (user && !this.tokenStorage.isTokenExpired()) {
      this.currentUserSubject.next(user);
      this.scheduleTokenRefresh();
      this.logger.info('[AUTH_INIT] Session restored', { username: user.username });
    } else if (user) {
      this.logger.info('[AUTH_INIT] Session expired, clearing tokens');
      this.tokenStorage.clear();
    }
  }

  /**
   * Authenticate user with username and password
   * @param credentials Login credentials
   * @param rememberMe If true, persist tokens in localStorage
   * @returns Observable of AuthResponse
   */
  login(credentials: LoginRequest, rememberMe: boolean = false): Observable<AuthResponse> {
    this.logger.info('[AUTH_LOGIN] Login attempt', { username: credentials.username });

    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.handleAuthenticationSuccess(response, rememberMe);
        this.logger.info('[AUTH_LOGIN] Login successful', { username: credentials.username });
      }),
      catchError(error => {
        this.logger.error('[AUTH_LOGIN] Login failed', {
          username: credentials.username,
          status: error.status,
          message: error.message
        });
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout current user
   * Revokes refresh token on backend and clears local state
   */
  logout(): void {
    const refreshToken = this.tokenStorage.getRefreshToken();

    if (refreshToken) {
      this.logger.info('[AUTH_LOGOUT] Logout initiated');

      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).pipe(
        catchError(error => {
          this.logger.warn('[AUTH_LOGOUT] Logout endpoint error (proceeding with local cleanup)', {
            status: error.status
          });
          return throwError(() => error);
        })
      ).subscribe({
        complete: () => {
          this.clearAuthenticationState();
          this.logger.info('[AUTH_LOGOUT] Logout completed');
        }
      });
    } else {
      this.clearAuthenticationState();
      this.logger.info('[AUTH_LOGOUT] Local logout (no refresh token)');
    }
  }

  /**
   * Refresh access token using refresh token
   * @returns Observable of new AuthResponse
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();

    if (!refreshToken) {
      this.logger.warn('[AUTH_REFRESH] No refresh token available');
      this.clearAuthenticationState();
      return throwError(() => new Error('No refresh token available'));
    }

    if (this.isRefreshing) {
      this.logger.debug('[AUTH_REFRESH] Refresh already in progress');
      return throwError(() => new Error('Refresh already in progress'));
    }

    this.isRefreshing = true;
    this.logger.debug('[AUTH_REFRESH] Refreshing access token');

    const request: TokenRefreshRequest = { refreshToken };

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, request).pipe(
      tap(response => {
        const rememberMe = this.tokenStorage.isRememberMeEnabled();
        this.handleAuthenticationSuccess(response, rememberMe);
        this.isRefreshing = false;
        this.logger.info('[AUTH_REFRESH] Token refresh successful');
      }),
      catchError(error => {
        this.isRefreshing = false;
        this.logger.error('[AUTH_REFRESH] Token refresh failed', {
          status: error.status,
          message: error.message
        });
        this.clearAuthenticationState();
        return throwError(() => error);
      })
    );
  }

  /**
   * Check if user is currently authenticated
   * @returns True if valid token exists
   */
  isAuthenticated(): boolean {
    const token = this.tokenStorage.getAccessToken();
    return token !== null && !this.tokenStorage.isTokenExpired();
  }

  /**
   * Get current access token
   * @returns Access token or null
   */
  getToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  /**
   * Get current user
   * @returns Current user or null
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if current user has admin role
   * @returns True if user is admin
   */
  isAdmin(): boolean {
    return this.getCurrentUser()?.isAdmin ?? false;
  }

  /**
   * Handle successful authentication
   * Save tokens, update user state, and schedule refresh
   */
  private handleAuthenticationSuccess(response: AuthResponse, rememberMe: boolean): void {
    this.tokenStorage.saveTokens(response, rememberMe);
    const user = this.tokenStorage.getCurrentUser();
    this.currentUserSubject.next(user);
    this.scheduleTokenRefresh();
  }

  /**
   * Clear authentication state and navigate to login
   */
  private clearAuthenticationState(): void {
    this.cancelTokenRefresh();
    this.tokenStorage.clear();
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  /**
   * Schedule automatic token refresh before expiration
   * Refreshes token 60 seconds before it expires
   */
  private scheduleTokenRefresh(): void {
    this.cancelTokenRefresh();

    const timeUntilExpiration = this.tokenStorage.getTimeUntilExpiration();
    const refreshTime = timeUntilExpiration - (this.REFRESH_BUFFER_SECONDS * 1000);

    if (refreshTime > 0) {
      this.logger.debug('[AUTH_SCHEDULE] Scheduling token refresh', {
        refreshInMs: refreshTime,
        refreshInMinutes: Math.round(refreshTime / 60000)
      });

      this.refreshTimerSubscription = timer(refreshTime).pipe(
        switchMap(() => this.refreshToken())
      ).subscribe({
        error: (error) => {
          this.logger.error('[AUTH_SCHEDULE] Scheduled refresh failed', {
            error: error.message
          });
        }
      });
    } else {
      this.logger.debug('[AUTH_SCHEDULE] Token expires soon, refreshing immediately');
      this.refreshToken().subscribe();
    }
  }

  /**
   * Cancel scheduled token refresh
   */
  private cancelTokenRefresh(): void {
    if (this.refreshTimerSubscription) {
      this.refreshTimerSubscription.unsubscribe();
      this.refreshTimerSubscription = undefined;
      this.logger.debug('[AUTH_SCHEDULE] Token refresh cancelled');
    }
  }
}
