import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { interval, Subscription } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { TokenStorageService } from '../../../services/token-storage.service';
import { LoggerService } from '../../../services/logger.service';
import { ModalService } from '../../../services/modal.service';

/**
 * Session Expiry Warning Modal
 * Displays a warning modal when session is about to expire
 * Allows user to extend session or logout gracefully
 */
@Component({
  selector: 'app-session-expiry-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './session-expiry-modal.component.html',
  styleUrls: ['./session-expiry-modal.component.css'],
})
export class SessionExpiryModalComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly logger = inject(LoggerService);
  private readonly modalService = inject(ModalService);

  private checkSubscription?: Subscription;
  private countdownSubscription?: Subscription;

  private readonly WARNING_THRESHOLD_MS = 120000; // Show warning 2 minutes before expiry

  isVisible = false;
  secondsRemaining = 0;

  ngOnInit(): void {
    this.startMonitoring();
  }

  ngOnDestroy(): void {
    this.stopMonitoring();
  }

  /**
   * Start monitoring token expiration
   */
  private startMonitoring(): void {
    // Check every 10 seconds if we should show warning
    this.checkSubscription = interval(10000).subscribe(() => {
      if (!this.authService.isAuthenticated()) {
        this.hideModal();
        return;
      }

      const timeUntilExpiration = this.tokenStorage.getTimeUntilExpiration();

      if (
        timeUntilExpiration > 0 &&
        timeUntilExpiration <= this.WARNING_THRESHOLD_MS &&
        !this.isVisible
      ) {
        this.showWarning();
      } else if (timeUntilExpiration <= 0 && this.isVisible) {
        this.onSessionExpired();
      }
    });
  }

  /**
   * Stop monitoring
   */
  private stopMonitoring(): void {
    if (this.checkSubscription) {
      this.checkSubscription.unsubscribe();
    }
    if (this.countdownSubscription) {
      this.countdownSubscription.unsubscribe();
    }
  }

  /**
   * Show expiry warning modal
   */
  private showWarning(): void {
    this.isVisible = true;
    this.logger.warn('[SESSION_EXPIRY] Showing session expiry warning');

    // Start countdown
    this.countdownSubscription = interval(1000).subscribe(() => {
      const timeUntilExpiration = this.tokenStorage.getTimeUntilExpiration();
      this.secondsRemaining = Math.max(0, Math.floor(timeUntilExpiration / 1000));

      if (this.secondsRemaining <= 0) {
        this.onSessionExpired();
      }
    });
  }

  /**
   * Hide modal
   */
  private hideModal(): void {
    this.isVisible = false;
    if (this.countdownSubscription) {
      this.countdownSubscription.unsubscribe();
      this.countdownSubscription = undefined;
    }
  }

  /**
   * User chose to extend session
   */
  onExtendSession(): void {
    this.logger.info('[SESSION_EXPIRY] User extended session');
    this.hideModal();

    this.authService.refreshToken().subscribe({
      next: () => {
        this.logger.info('[SESSION_EXPIRY] Session extended successfully');
      },
      error: (error) => {
        this.logger.error('[SESSION_EXPIRY] Failed to extend session', {
          error: error.message,
        });
        this.authService.logout();
      },
    });
  }

  /**
   * User chose to logout
   */
  onLogout(): void {
    this.logger.info('[SESSION_EXPIRY] User chose to logout');
    this.hideModal();
    this.authService.logout();
  }

  /**
   * Session has expired
   */
  private onSessionExpired(): void {
    this.logger.warn('[SESSION_EXPIRY] Session expired');
    this.hideModal();
    this.authService.logout();
  }

  /**
   * Format seconds as MM:SS
   */
  get formattedTime(): string {
    const minutes = Math.floor(this.secondsRemaining / 60);
    const seconds = this.secondsRemaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
