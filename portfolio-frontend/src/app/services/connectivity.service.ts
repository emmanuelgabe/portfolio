import { Injectable, signal, computed, NgZone, inject, OnDestroy } from '@angular/core';
import { LoggerService } from './logger.service';

/**
 * Service to track online/offline connectivity status.
 * Uses browser's navigator.onLine and online/offline events.
 */
@Injectable({ providedIn: 'root' })
export class ConnectivityService implements OnDestroy {
  private readonly ngZone = inject(NgZone);
  private readonly logger = inject(LoggerService);

  private readonly onlineStatus = signal(navigator.onLine);

  readonly isOnline = this.onlineStatus.asReadonly();
  readonly isOffline = computed(() => !this.onlineStatus());

  private readonly onlineHandler = (): void => {
    this.ngZone.run(() => {
      this.onlineStatus.set(true);
      this.logger.info('[CONNECTIVITY] Connection restored');
    });
  };

  private readonly offlineHandler = (): void => {
    this.ngZone.run(() => {
      this.onlineStatus.set(false);
      this.logger.warn('[CONNECTIVITY] Connection lost');
    });
  };

  constructor() {
    this.setupEventListeners();
  }

  private setupEventListeners(): void {
    window.addEventListener('online', this.onlineHandler);
    window.addEventListener('offline', this.offlineHandler);
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.onlineHandler);
    window.removeEventListener('offline', this.offlineHandler);
  }
}
