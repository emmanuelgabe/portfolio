import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

export interface CircuitBreakerMetrics {
  failureCount: number;
  successCount: number;
  bufferedCalls: number;
  failureRate: number;
  notPermittedCalls: number; // long in backend but number in TypeScript
}

export interface CircuitBreakerStatus {
  name: string;
  state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' | 'DISABLED' | 'FORCED_OPEN';
  metrics: CircuitBreakerMetrics;
  timestamp: string;
}

@Injectable({
  providedIn: 'root',
})
export class CircuitBreakerService {
  private readonly http = inject(HttpClient);
  private readonly logger = inject(LoggerService);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/circuit-breakers`;

  getAllCircuitBreakers(): Observable<CircuitBreakerStatus[]> {
    return this.http.get<CircuitBreakerStatus[]>(this.apiUrl).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch circuit breakers failed', {
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }

  getCircuitBreaker(name: string): Observable<CircuitBreakerStatus> {
    return this.http.get<CircuitBreakerStatus>(`${this.apiUrl}/${name}`).pipe(
      catchError((error) => {
        this.logger.error('[HTTP_ERROR] Fetch circuit breaker failed', {
          name,
          status: error.status,
          message: error.message,
        });
        return throwError(() => error);
      })
    );
  }
}
