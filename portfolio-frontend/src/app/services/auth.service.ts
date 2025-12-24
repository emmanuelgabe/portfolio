import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError, timer, Subscription, Subject, of } from 'rxjs';
import { tap, catchError, switchMap, retry } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import { LoginRequest, AccessTokenResponse, User } from '../models/auth.model';

/**
 * Authentication service with JWT token management.
 *
 * Security architecture:
 * - Access token: stored in memory only (TokenStorageService)
 * - Refresh token: stored in HttpOnly cookie (managed by backend)
 *
 * This hybrid approach protects against XSS attacks while maintaining
 * a seamless user experience with automatic session restoration.
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
  private refreshSubject = new Subject<AccessTokenResponse>();
  private sessionRestoreAttempted = false;

  constructor() {
    this.initializeAuthState();
  }

  /**
   * Initialize authentication state on service creation.
   * If access token exists in memory and is valid, restore session.
   * If no token in memory, attempt to restore via refresh token cookie.
   */
  private initializeAuthState(): void {
    const user = this.tokenStorage.getCurrentUser();

    if (user && !this.tokenStorage.isTokenExpired()) {
      // Token in memory is still valid
      this.currentUserSubject.next(user);
      this.scheduleTokenRefresh();
      this.logger.info('[AUTH_INIT] Session restored from memory', { username: user.username });
    } else if (this.tokenStorage.shouldAttemptSessionRestore()) {
      // No token in memory - try to restore via refresh token cookie
      this.logger.info('[AUTH_INIT] Attempting session restore via refresh token');
      this.attemptSessionRestore();
    } else {
      this.logger.debug('[AUTH_INIT] No session to restore');
    }
  }

  /**
   * Attempt to restore session using refresh token from HttpOnly cookie.
   * Called on app initialization when no access token is in memory.
   */
  private attemptSessionRestore(): void {
    if (this.sessionRestoreAttempted) {
      return;
    }
    this.sessionRestoreAttempted = true;

    this.refreshToken().subscribe({
      next: () => {
        this.logger.info('[AUTH_INIT] Session restored via refresh token');
      },
      error: () => {
        this.logger.debug('[AUTH_INIT] No valid session to restore');
      },
    });
  }

  /**
   * Authenticate user with username and password.
   * Access token is returned in response body and stored in memory.
   * Refresh token is set as HttpOnly cookie by the backend.
   *
   * @param credentials Login credentials
   * @returns Observable of AccessTokenResponse
   */
  login(credentials: LoginRequest): Observable<AccessTokenResponse> {
    this.logger.info('[AUTH_LOGIN] Login attempt', { username: credentials.username });

    return this.http
      .post<AccessTokenResponse>(`${this.apiUrl}/login`, credentials, {
        withCredentials: true, // Required for cookies
      })
      .pipe(
        tap((response) => {
          this.handleAuthenticationSuccess(response);
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
   * Logout current user.
   * Clears HttpOnly cookie via backend endpoint and local state.
   */
  logout(): void {
    this.logger.info('[AUTH_LOGOUT] Logout initiated');

    this.http
      .post(
        `${this.apiUrl}/logout`,
        {},
        {
          withCredentials: true, // Required to send/clear cookies
        }
      )
      .pipe(
        catchError((error) => {
          this.logger.warn('[AUTH_LOGOUT] Logout endpoint error (proceeding with local cleanup)', {
            status: error.status,
          });
          return of(null);
        })
      )
      .subscribe({
        complete: () => {
          this.clearAuthenticationState();
          this.logger.info('[AUTH_LOGOUT] Logout completed');
        },
      });
  }

  /**
   * Refresh access token using refresh token from HttpOnly cookie.
   * Uses synchronous flag to prevent race conditions from concurrent calls.
   *
   * @returns Observable of new AccessTokenResponse
   */
  refreshToken(): Observable<AccessTokenResponse> {
    if (this.isRefreshing) {
      this.logger.debug('[AUTH_REFRESH] Refresh already in progress, waiting for completion');
      return this.refreshSubject.asObservable();
    }

    this.isRefreshing = true;
    this.logger.debug('[AUTH_REFRESH] Refreshing access token');

    return this.http
      .post<AccessTokenResponse>(
        `${this.apiUrl}/refresh`,
        {},
        {
          withCredentials: true, // Required to send refresh token cookie
        }
      )
      .pipe(
        tap((response) => {
          this.tokenStorage.saveTokens(response);
          const user = this.tokenStorage.getCurrentUser();
          this.currentUserSubject.next(user);

          this.scheduleTokenRefresh();
          this.logger.info('[AUTH_REFRESH] Token refresh successful');

          this.refreshSubject.next(response);
          this.refreshSubject.complete();

          this.refreshSubject = new Subject<AccessTokenResponse>();
          this.isRefreshing = false;
        }),
        catchError((error) => {
          this.logger.error('[AUTH_REFRESH] Token refresh failed', {
            status: error.status,
            message: error.message,
          });

          this.refreshSubject.error(error);
          this.refreshSubject = new Subject<AccessTokenResponse>();
          this.isRefreshing = false;

          // Clear state only if user is currently logged in
          // Skip clearing during initial silent restore (when no user is logged in yet)
          if (this.currentUserSubject.value !== null) {
            this.clearAuthenticationState();
          }
          return throwError(() => error);
        })
      );
  }

  /**
   * Check if user is currently authenticated.
   * @returns True if valid token exists in memory
   */
  isAuthenticated(): boolean {
    return this.tokenStorage.hasValidToken();
  }

  /**
   * Get current access token from memory.
   * @returns Access token or null
   */
  getToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  /**
   * Get current user.
   * @returns Current user or null
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if current user has admin role.
   * @returns True if user is admin
   */
  isAdmin(): boolean {
    return this.getCurrentUser()?.isAdmin ?? false;
  }

  /**
   * Handle successful authentication.
   * Saves tokens in memory, updates user state, and schedules refresh.
   */
  private handleAuthenticationSuccess(response: AccessTokenResponse): void {
    this.tokenStorage.saveTokens(response);
    const user = this.tokenStorage.getCurrentUser();
    this.currentUserSubject.next(user);
    this.scheduleTokenRefresh();
  }

  /**
   * Clear authentication state and navigate to login.
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
   * Schedule automatic token refresh before expiration.
   * Applies 60-second buffer and minimum 2-minute threshold.
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
   * Cancel scheduled token refresh.
   */
  private cancelTokenRefresh(): void {
    if (this.refreshTimerSubscription) {
      this.refreshTimerSubscription.unsubscribe();
      this.refreshTimerSubscription = undefined;
      this.logger.debug('[AUTH_SCHEDULE] Token refresh cancelled');
    }
  }
}
