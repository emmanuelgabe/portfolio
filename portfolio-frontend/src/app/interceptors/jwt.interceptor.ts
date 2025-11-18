import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';

/**
 * JWT HTTP Interceptor
 * Automatically attaches Authorization header with JWT token to outgoing requests
 * Handles 401 errors by attempting token refresh and retrying the request
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const logger = inject(LoggerService);

  const token = authService.getToken();
  const isAuthEndpoint = req.url.includes('/api/auth/');

  // Clone request and add Authorization header if token exists and not auth endpoint
  let authReq = req;
  if (token && !isAuthEndpoint) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });

    logger.debug('[JWT_INTERCEPTOR] Added Authorization header', {
      method: req.method,
      url: req.url,
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized errors
      if (error.status === 401 && !isAuthEndpoint) {
        logger.warn('[JWT_INTERCEPTOR] 401 Unauthorized, attempting token refresh', {
          url: req.url,
        });

        // Attempt to refresh token and retry request
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Get new token after refresh
            const newToken = authService.getToken();

            if (newToken) {
              logger.info('[JWT_INTERCEPTOR] Token refreshed, retrying request', {
                url: req.url,
              });

              // Clone original request with new token
              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${newToken}`,
                },
              });

              return next(retryReq);
            } else {
              logger.error('[JWT_INTERCEPTOR] No token after refresh');
              return throwError(() => error);
            }
          }),
          catchError((refreshError) => {
            // Refresh failed, logout user
            logger.error('[JWT_INTERCEPTOR] Token refresh failed, redirecting to login', {
              status: refreshError.status,
              message: refreshError.message,
            });

            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }

      // Handle 403 Forbidden errors
      if (error.status === 403) {
        logger.warn('[JWT_INTERCEPTOR] 403 Forbidden - insufficient permissions', {
          url: req.url,
        });
      }

      return throwError(() => error);
    })
  );
};
