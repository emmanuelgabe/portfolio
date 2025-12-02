import { Injectable, inject } from '@angular/core';
import { fromEvent, merge, throttleTime, Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { LoggerService } from './logger.service';

/**
 * Activity Monitor Service
 * Detects user activity and refreshes tokens proactively when user is active
 * Prevents session expiration during active usage
 */
@Injectable({
  providedIn: 'root',
})
export class ActivityMonitorService {
  private readonly authService = inject(AuthService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly logger = inject(LoggerService);

  private activitySubscription?: Subscription;
  private lastRefreshTriggerTime = 0;
  private readonly ACTIVITY_THROTTLE_MS = 30000; // Check activity max once every 30 seconds
  private readonly REFRESH_THRESHOLD_MS = 300000; // Refresh if token expires in less than 5 minutes
  private readonly MIN_REFRESH_INTERVAL_MS = 60000; // Minimum 60 seconds between activity-triggered refreshes

  /**
   * Start monitoring user activity
   * Will automatically refresh token when user is active and token expires soon
   */
  startMonitoring(): void {
    if (this.activitySubscription) {
      this.logger.debug('[ACTIVITY_MONITOR] Already monitoring user activity');
      return;
    }

    this.logger.info('[ACTIVITY_MONITOR] Started monitoring user activity');

    // Monitor various user activity events
    const click$ = fromEvent(document, 'click');
    const keypress$ = fromEvent(document, 'keypress');
    const mousemove$ = fromEvent(document, 'mousemove');
    const scroll$ = fromEvent(document, 'scroll');

    // Merge all activity events and throttle to avoid excessive checks
    this.activitySubscription = merge(click$, keypress$, mousemove$, scroll$)
      .pipe(throttleTime(this.ACTIVITY_THROTTLE_MS))
      .subscribe(() => {
        this.onUserActivity();
      });
  }

  /**
   * Stop monitoring user activity
   */
  stopMonitoring(): void {
    if (this.activitySubscription) {
      this.activitySubscription.unsubscribe();
      this.activitySubscription = undefined;
      this.logger.info('[ACTIVITY_MONITOR] Stopped monitoring user activity');
    }
  }

  /**
   * Handle detected user activity
   * Refreshes token if user is active and token expires soon
   */
  private onUserActivity(): void {
    if (!this.authService.isAuthenticated()) {
      return;
    }

    const timeUntilExpiration = this.tokenStorage.getTimeUntilExpiration();
    const now = Date.now();
    const timeSinceLastRefresh = now - this.lastRefreshTriggerTime;

    // Check if token expires soon
    if (timeUntilExpiration > 0 && timeUntilExpiration < this.REFRESH_THRESHOLD_MS) {
      // Avoid triggering refresh too frequently
      if (timeSinceLastRefresh < this.MIN_REFRESH_INTERVAL_MS) {
        this.logger.debug(
          '[ACTIVITY_MONITOR] Token expires soon but refresh triggered recently, skipping',
          {
            expiresInMs: timeUntilExpiration,
            timeSinceLastRefreshMs: timeSinceLastRefresh,
          }
        );
        return;
      }

      this.logger.info('[ACTIVITY_MONITOR] User active and token expires soon, refreshing', {
        expiresInMs: timeUntilExpiration,
        expiresInMinutes: Math.round(timeUntilExpiration / 60000),
      });

      this.lastRefreshTriggerTime = now;

      this.authService.refreshToken().subscribe({
        next: () => {
          this.logger.info('[ACTIVITY_MONITOR] Token refreshed successfully due to activity');
        },
        error: (error) => {
          this.logger.error('[ACTIVITY_MONITOR] Failed to refresh token on activity', {
            error: error.message,
          });
        },
      });
    }
  }
}
