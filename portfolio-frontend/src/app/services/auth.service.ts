import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, timer, Subscription, Subject } from 'rxjs';
import { tap, catchError, switchMap, retry } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import { LoginRequest, AuthResponse, TokenRefreshRequest, User } from '../models/auth.model';

/**
 * Authentication service with JWT token management
 * Handles login, logout, token refresh, and auto-refresh scheduling
 */
@Injectable({
  providedIn: 'root',
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
  private refreshSubject = new Subject<AuthResponse>();

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
      tap((response) => {
        this.handleAuthenticationSuccess(response, rememberMe);
        this.logger.info('[AUTH_LOGIN] Login successful', { username: credentials.username });
      }),
      catchError((error) => {
        this.logger.error('[AUTH_LOGIN] Login failed', {
          username: credentials.username,
          status: error.status,
          message: error.message,
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

      this.http
        .post(`${this.apiUrl}/logout`, { refreshToken })
        .pipe(
          catchError((error) => {
            this.logger.warn(
              '[AUTH_LOGOUT] Logout endpoint error (proceeding with local cleanup)',
              {
                status: error.status,
              }
            );
            return throwError(() => error);
          })
        )
        .subscribe({
          complete: () => {
            this.clearAuthenticationState();
            this.logger.info('[AUTH_LOGOUT] Logout completed');
          },
        });
    } else {
      this.clearAuthenticationState();
      this.logger.info('[AUTH_LOGOUT] Local logout (no refresh token)');
    }
  }

  /**
   * Refresh access token using refresh token
   * Uses synchronous flag to prevent race conditions from concurrent calls
   * @returns Observable of new AuthResponse
   */
  refreshToken(): Observable<AuthResponse> {
    if (this.isRefreshing) {
      this.logger.debug('[AUTH_REFRESH] Refresh already in progress, waiting for completion');
      return this.refreshSubject.asObservable();
    }

    const refreshToken = this.tokenStorage.getRefreshToken();

    if (!refreshToken) {
      this.logger.warn('[AUTH_REFRESH] No refresh token available');
      this.clearAuthenticationState();
      return throwError(() => new Error('No refresh token available'));
    }

    this.isRefreshing = true;
    this.logger.debug('[AUTH_REFRESH] Refreshing access token');

    const request: TokenRefreshRequest = { refreshToken };

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, request).pipe(
      tap((response) => {
        const rememberMe = this.tokenStorage.isRememberMeEnabled();
        this.tokenStorage.saveTokens(response, rememberMe);
        const user = this.tokenStorage.getCurrentUser();
        this.currentUserSubject.next(user);

        this.scheduleTokenRefresh();
        this.logger.info('[AUTH_REFRESH] Token refresh successful');

        this.refreshSubject.next(response);
        this.refreshSubject.complete();

        this.refreshSubject = new Subject<AuthResponse>();
        this.isRefreshing = false;
      }),
      catchError((error) => {
        this.logger.error('[AUTH_REFRESH] Token refresh failed', {
          status: error.status,
          message: error.message,
        });

        this.refreshSubject.error(error);
        this.refreshSubject = new Subject<AuthResponse>();
        this.isRefreshing = false;

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
   * Saves tokens, updates user state, and schedules refresh
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

    // Don't redirect to login if in demo mode
    const currentUrl = this.router.url;
    if (!currentUrl.startsWith('/admindemo')) {
      this.router.navigate(['/login']);
    }
  }

  /**
   * Schedule automatic token refresh before expiration
   * Applies 60-second buffer and minimum 2-minute threshold
   */
  private scheduleTokenRefresh(): void {
    this.cancelTokenRefresh();

    const timeUntilExpiration = this.tokenStorage.getTimeUntilExpiration();
    const refreshTime = timeUntilExpiration - this.REFRESH_BUFFER_SECONDS * 1000;
    const MIN_REFRESH_TIME = 120000;

    if (refreshTime < MIN_REFRESH_TIME) {
      this.logger.warn('[AUTH_SCHEDULE] Token expires too soon, not scheduling automatic refresh', {
        timeUntilExpirationMs: timeUntilExpiration,
        timeUntilExpirationMin: Math.round(timeUntilExpiration / 60000),
        refreshTimeMs: refreshTime,
        minRequiredMs: MIN_REFRESH_TIME,
      });
      return;
    }

    this.logger.info('[AUTH_SCHEDULE] Scheduling token refresh', {
      refreshInMs: refreshTime,
      refreshInMinutes: Math.round(refreshTime / 60000),
    });

    this.refreshTimerSubscription = timer(refreshTime)
      .pipe(
        switchMap(() => {
          this.logger.info('[AUTH_SCHEDULE] Executing scheduled token refresh');
          return this.refreshToken();
        }),
        retry({
          count: 2,
          delay: 5000,
        })
      )
      .subscribe({
        error: (error) => {
          this.logger.error('[AUTH_SCHEDULE] Scheduled refresh failed after retries', {
            error: error.message,
          });
        },
      });
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
