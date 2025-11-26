import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * JWT HTTP Interceptor
 * Automatically attaches Authorization header with JWT token to outgoing requests
 * Handles 401 errors by attempting token refresh and retrying the request
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const logger = inject(LoggerService);
  const tokenStorage = inject(TokenStorageService);

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

      // Handle 403 Forbidden errors - might be expired token
      if (error.status === 403 && !isAuthEndpoint) {
        logger.warn('[JWT_INTERCEPTOR] 403 Forbidden - checking if token is expired', {
          url: req.url,
        });

        // Check if token is expired (backend might return 403 instead of 401)
        if (tokenStorage.isTokenExpired(0)) {
          logger.info('[JWT_INTERCEPTOR] Token is expired, attempting refresh despite 403');

          // Attempt to refresh token and retry request
          return authService.refreshToken().pipe(
            switchMap(() => {
              const newToken = authService.getToken();

              if (newToken) {
                logger.info('[JWT_INTERCEPTOR] Token refreshed after 403, retrying request', {
                  url: req.url,
                });

                const retryReq = req.clone({
                  setHeaders: {
                    Authorization: `Bearer ${newToken}`,
                  },
                });

                return next(retryReq);
              }
              return throwError(() => error);
            }),
            catchError((_refreshError) => {
              logger.error('[JWT_INTERCEPTOR] Token refresh failed on 403 fallback');
              authService.logout();
              return throwError(() => error);
            })
          );
        } else {
          logger.warn('[JWT_INTERCEPTOR] 403 Forbidden - genuine permission issue', {
            url: req.url,
          });
        }
      }

      return throwError(() => error);
    })
  );
};
