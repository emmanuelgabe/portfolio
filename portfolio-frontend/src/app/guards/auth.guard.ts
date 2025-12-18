import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';

/**
 * Authentication guard for route protection
 * Checks if user is authenticated before allowing access to protected routes
 * Redirects to login page with returnUrl if not authenticated
 */
export const authGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const logger = inject(LoggerService);

  const isAuthenticated = authService.isAuthenticated();
  const url = state.url;

  if (isAuthenticated) {
    logger.debug('[AUTH_GUARD] Access granted', { url });
    return true;
  }

  logger.warn('[AUTH_GUARD] Access denied, redirecting to login', {
    url,
    returnUrl: url,
  });

  // Redirect to login with return URL
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: url },
  });
};

/**
 * Admin guard for admin-only routes
 * Checks if user is authenticated AND has admin role
 * Redirects to home if not admin, or login if not authenticated
 */
export const adminGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const logger = inject(LoggerService);

  const isAuthenticated = authService.isAuthenticated();
  const isAdmin = authService.isAdmin();
  const url = state.url;

  if (isAuthenticated && isAdmin) {
    logger.debug('[ADMIN_GUARD] Admin access granted', { url });
    return true;
  }

  if (!isAuthenticated) {
    logger.warn('[ADMIN_GUARD] Not authenticated, redirecting to login', {
      url,
      returnUrl: url,
    });

    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: url },
    });
  }

  logger.warn('[ADMIN_GUARD] Insufficient permissions, redirecting to error 403', {
    url,
    isAdmin,
  });

  return router.createUrlTree(['/error/403']);
};
