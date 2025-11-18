import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { LoggerService } from '../services/logger.service';

/**
 * HTTP Interceptor for logging all HTTP requests and responses
 * Provides centralized logging for debugging and monitoring
 */
export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const logger = inject(LoggerService);
  const startTime = Date.now();

  logger.debug('[HTTP_INTERCEPTOR] Request started', {
    method: req.method,
    url: req.url,
  });

  return next(req).pipe(
    tap((_event) => {
      const duration = Date.now() - startTime;
      logger.debug('[HTTP_INTERCEPTOR] Request completed', {
        method: req.method,
        url: req.url,
        duration: `${duration}ms`,
      });
    }),
    catchError((error) => {
      const duration = Date.now() - startTime;
      logger.error('[HTTP_INTERCEPTOR] Request failed', {
        method: req.method,
        url: req.url,
        status: error.status,
        statusText: error.statusText,
        duration: `${duration}ms`,
      });
      return throwError(() => error);
    })
  );
};
