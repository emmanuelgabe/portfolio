import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { retry, timer } from 'rxjs';
import { LoggerService } from '../services/logger.service';

/**
 * HTTP Interceptor for retrying failed requests
 *
 * Automatically retries requests that fail due to connection issues (ECONNREFUSED)
 * which typically occur during application startup when backend is not yet ready.
 *
 * Configuration:
 * - Retries: 3 attempts
 * - Delay: 1 second between attempts
 * - Only retries connection errors (not 4xx/5xx HTTP errors)
 */
export const retryInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(LoggerService);

  return next(req).pipe(
    retry({
      count: 3,
      delay: (error: HttpErrorResponse, retryCount: number) => {
        // Only retry on connection errors (ECONNREFUSED, network issues)
        // Don't retry on HTTP errors (4xx, 5xx) with actual responses
        if (error.status === 0 || error.status >= 500) {
          const delayMs = 1000; // 1 second delay

          logger.debug('[RETRY_INTERCEPTOR] Retrying request', {
            url: req.url,
            method: req.method,
            attempt: retryCount + 1,
            maxRetries: 3,
            delayMs: delayMs,
            errorStatus: error.status,
          });

          return timer(delayMs);
        }

        // Don't retry client errors (4xx)
        throw error;
      },
    })
  );
};
