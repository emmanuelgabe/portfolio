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

      // Log 404 errors as debug (expected for missing resources like CV)
      // Log other errors as error
      const logLevel = error.status === 404 ? 'debug' : 'error';
      const logMessage =
        error.status === 404
          ? '[HTTP_INTERCEPTOR] Resource not found (404)'
          : '[HTTP_INTERCEPTOR] Request failed';

      logger[logLevel](logMessage, {
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
