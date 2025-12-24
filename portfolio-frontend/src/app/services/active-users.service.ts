import { Injectable, inject, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';

export interface ActiveUsersResponse {
  count: number;
  timestamp: string;
}

export interface VisitorStatsResponse {
  activeCount: number;
  lastMonthCount: number;
  timestamp: string;
}

export interface DailyVisitorData {
  date: string;
  count: number;
}

/**
 * Active Users Service
 * Provides real-time active visitors count via Server-Sent Events (SSE).
 * Used in the admin dashboard to display live visitor statistics.
 */
@Injectable({
  providedIn: 'root',
})
export class ActiveUsersService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly ngZone = inject(NgZone);

  private readonly apiUrl = environment.apiUrl;
  private eventSource?: EventSource;
  private activeUsersSubject = new BehaviorSubject<number>(0);
  private lastMonthCountSubject = new BehaviorSubject<number>(0);
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_ATTEMPTS = 5;
  private readonly RECONNECT_DELAY_MS = 5000;

  /**
   * Get the current active users count as an observable.
   */
  get activeUsers$(): Observable<number> {
    return this.activeUsersSubject.asObservable();
  }

  /**
   * Get the connection status as an observable.
   */
  get connected$(): Observable<boolean> {
    return this.connectionStatus.asObservable();
  }

  /**
   * Get the last month visitor count as an observable.
   */
  get lastMonthCount$(): Observable<number> {
    return this.lastMonthCountSubject.asObservable();
  }

  /**
   * Fetch visitor stats (current active + last month total).
   * This is called once on component init to get historical data.
   */
  fetchVisitorStats(): void {
    this.http
      .get<VisitorStatsResponse>(`${this.apiUrl}/api/admin/visitors/stats`)
      .pipe(
        catchError((error) => {
          this.logger.error('[ACTIVE_USERS] Failed to fetch visitor stats', {
            status: error.status,
            message: error.message,
          });
          return of(null);
        })
      )
      .subscribe((response) => {
        if (response) {
          this.lastMonthCountSubject.next(response.lastMonthCount);
          this.logger.debug('[ACTIVE_USERS] Visitor stats loaded', {
            lastMonthCount: response.lastMonthCount,
          });
        }
      });
  }

  /**
   * Connect to the SSE stream.
   * Note: EventSource doesn't support custom headers, so we use a workaround
   * by fetching with auth then establishing the stream.
   */
  connect(): void {
    if (this.eventSource) {
      this.logger.debug('[ACTIVE_USERS] Already connected');
      return;
    }

    const token = this.tokenStorage.getAccessToken();
    if (!token) {
      this.logger.warn('[ACTIVE_USERS] No access token available');
      return;
    }

    this.logger.info('[ACTIVE_USERS] Connecting to SSE stream');

    // Use fetch with streaming for authenticated SSE
    this.connectWithFetch(token);
  }

  /**
   * Connect using fetch API with streaming for authenticated SSE.
   */
  private async connectWithFetch(token: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/api/admin/visitors/stream`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      if (!response.body) {
        throw new Error('No response body');
      }

      this.connectionStatus.next(true);
      this.reconnectAttempts = 0;
      this.logger.info('[ACTIVE_USERS] Connected to SSE stream');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          this.logger.info('[ACTIVE_USERS] Stream ended');
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data) {
              this.handleMessage(data);
            }
          }
        }
      }

      this.handleDisconnect();
    } catch (error) {
      this.logger.error('[ACTIVE_USERS] Connection error', { error });
      this.handleDisconnect();
    }
  }

  /**
   * Handle incoming SSE message.
   */
  private handleMessage(data: string): void {
    try {
      const response: ActiveUsersResponse = JSON.parse(data);
      this.ngZone.run(() => {
        this.activeUsersSubject.next(response.count);
      });
      this.logger.debug('[ACTIVE_USERS] Received update', { count: response.count });
    } catch {
      this.logger.error('[ACTIVE_USERS] Failed to parse message', { data });
    }
  }

  /**
   * Handle disconnect and attempt reconnection.
   */
  private handleDisconnect(): void {
    this.connectionStatus.next(false);

    if (this.reconnectAttempts < this.MAX_RECONNECT_ATTEMPTS) {
      this.reconnectAttempts++;
      this.logger.info('[ACTIVE_USERS] Reconnecting', {
        attempt: this.reconnectAttempts,
        maxAttempts: this.MAX_RECONNECT_ATTEMPTS,
      });

      setTimeout(() => {
        const token = this.tokenStorage.getAccessToken();
        if (token) {
          this.connectWithFetch(token);
        }
      }, this.RECONNECT_DELAY_MS);
    } else {
      this.logger.warn('[ACTIVE_USERS] Max reconnect attempts reached');
    }
  }

  /**
   * Disconnect from the SSE stream.
   */
  disconnect(): void {
    this.connectionStatus.next(false);
    this.reconnectAttempts = this.MAX_RECONNECT_ATTEMPTS; // Prevent reconnection
    this.logger.info('[ACTIVE_USERS] Disconnected');
  }

  /**
   * Get current count (non-streaming, for initial load).
   */
  getCurrentCount(): number {
    return this.activeUsersSubject.getValue();
  }

  /**
   * Fetch daily visitor data for the last 7 days.
   * Used for displaying the visitors chart in the dashboard.
   */
  getDailyVisitorData(): Observable<DailyVisitorData[]> {
    return this.http.get<DailyVisitorData[]>(`${this.apiUrl}/api/admin/visitors/daily`).pipe(
      catchError((error) => {
        this.logger.error('[ACTIVE_USERS] Failed to fetch daily data', {
          status: error.status,
          message: error.message,
        });
        return of([]);
      })
    );
  }
}
