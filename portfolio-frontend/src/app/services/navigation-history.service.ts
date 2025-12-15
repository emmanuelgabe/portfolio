import { Injectable, OnDestroy, NgZone, PLATFORM_ID, inject } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Location, isPlatformBrowser } from '@angular/common';
import { Subject, fromEvent } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { LoggerService } from './logger.service';

/**
 * Navigation History Service
 * Tracks internal navigation history and provides smart back navigation
 * Handles browser/mobile back button to navigate within app hierarchy
 */
@Injectable({
  providedIn: 'root',
})
export class NavigationHistoryService implements OnDestroy {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly logger = inject(LoggerService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly ngZone = inject(NgZone);

  private readonly destroy$ = new Subject<void>();
  private readonly navigationStack: string[] = [];
  private readonly APP_STATE_KEY = 'portfolioAppNavigation';
  private readonly MAX_STACK_SIZE = 50;

  private isHandlingPopstate = false;
  private isInitialized = false;

  /**
   * Initialize navigation history tracking
   * Should be called once from AppComponent.ngOnInit()
   */
  initialize(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (this.isInitialized) {
      this.logger.debug('[NAVIGATION] Already initialized');
      return;
    }

    this.isInitialized = true;
    this.initializeHistoryState();
    this.setupRouterListener();
    this.setupPopstateListener();

    this.logger.info('[NAVIGATION] Navigation history service initialized');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get the parent route for a given URL based on app hierarchy
   * Returns null if no parent (allow exit)
   */
  getParentRoute(url: string): string | null {
    const cleanUrl = this.cleanUrl(url);

    // Project detail -> Home hero section
    if (/^\/projects\/\d+$/.test(cleanUrl)) {
      return '/#hero';
    }

    // Article detail -> Blog list
    if (/^\/blog\/[^/]+$/.test(cleanUrl)) {
      return '/blog';
    }

    // Blog list -> Home hero section
    if (cleanUrl === '/blog') {
      return '/#hero';
    }

    // Contact -> Home
    if (cleanUrl === '/contact') {
      return '/';
    }

    // Login -> Home
    if (cleanUrl === '/login') {
      return '/';
    }

    // Admin child routes -> Admin dashboard
    if (/^\/admin\/.+$/.test(cleanUrl)) {
      return '/admin';
    }

    // Demo child routes -> Demo dashboard
    if (/^\/admindemo\/.+$/.test(cleanUrl)) {
      return '/admindemo';
    }

    // Home or unknown - no parent (allow exit)
    return null;
  }

  /**
   * Check if there is internal navigation history
   */
  hasInternalHistory(): boolean {
    return this.navigationStack.length > 1;
  }

  /**
   * Mark the current history state as app-managed
   */
  private initializeHistoryState(): void {
    const currentState = history.state || {};
    if (!currentState[this.APP_STATE_KEY]) {
      const newState = { ...currentState, [this.APP_STATE_KEY]: true, stackIndex: 0 };
      history.replaceState(newState, '');
    }

    // Add initial route to stack
    const initialUrl = this.router.url;
    this.navigationStack.push(initialUrl);
    this.logger.debug('[NAVIGATION] Initial route added to stack', { url: initialUrl });
  }

  /**
   * Listen to router events and track navigation
   */
  private setupRouterListener(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event) => {
        if (this.isHandlingPopstate) {
          this.isHandlingPopstate = false;
          return;
        }

        this.addToStack(event.urlAfterRedirects);
      });
  }

  /**
   * Add a URL to the navigation stack
   */
  private addToStack(url: string): void {
    // Don't add duplicates
    if (this.navigationStack[this.navigationStack.length - 1] === url) {
      return;
    }

    this.navigationStack.push(url);

    // Limit stack size
    if (this.navigationStack.length > this.MAX_STACK_SIZE) {
      this.navigationStack.shift();
    }

    // Update history state with stack index
    const currentState = history.state || {};
    history.replaceState(
      { ...currentState, [this.APP_STATE_KEY]: true, stackIndex: this.navigationStack.length - 1 },
      ''
    );

    this.logger.debug('[NAVIGATION] Route added to stack', {
      url,
      stackSize: this.navigationStack.length,
    });
  }

  /**
   * Setup listener for browser/mobile back button
   */
  private setupPopstateListener(): void {
    this.ngZone.runOutsideAngular(() => {
      fromEvent<PopStateEvent>(window, 'popstate')
        .pipe(takeUntil(this.destroy$))
        .subscribe((event) => {
          this.ngZone.run(() => {
            this.handlePopstate(event);
          });
        });
    });
  }

  /**
   * Handle popstate event (back/forward button)
   */
  private handlePopstate(event: PopStateEvent): void {
    const state = event.state;
    const currentUrl = this.location.path() || '/';

    this.logger.debug('[NAVIGATION] Popstate event', {
      currentUrl,
      hasAppState: !!(state && state[this.APP_STATE_KEY]),
      stackSize: this.navigationStack.length,
    });

    // Check if this is a back navigation outside our tracked history
    if (!state || !state[this.APP_STATE_KEY]) {
      this.handleBackWithoutHistory(currentUrl);
      return;
    }

    // Check if going back in our stack
    const stateStackIndex = state.stackIndex;
    if (stateStackIndex !== undefined && stateStackIndex < this.navigationStack.length - 1) {
      // Remove entries from stack
      const entriesToRemove = this.navigationStack.length - 1 - stateStackIndex;
      for (let i = 0; i < entriesToRemove; i++) {
        this.navigationStack.pop();
      }
      this.logger.debug('[NAVIGATION] Stack adjusted after back navigation', {
        stackSize: this.navigationStack.length,
      });
    }
  }

  /**
   * Handle back navigation when there's no internal history
   */
  private handleBackWithoutHistory(currentUrl: string): void {
    const parentRoute = this.getParentRoute(currentUrl);

    if (parentRoute === null) {
      // No parent route - allow exit
      this.logger.debug('[NAVIGATION] No parent route, allowing exit', { currentUrl });
      return;
    }

    this.logger.info('[NAVIGATION] Redirecting to parent route', {
      from: currentUrl,
      to: parentRoute,
    });

    // Push state forward to prevent exit
    const newState = { [this.APP_STATE_KEY]: true, stackIndex: this.navigationStack.length };
    history.pushState(newState, '');

    // Navigate to parent
    this.isHandlingPopstate = true;
    this.navigateToParent(parentRoute);
  }

  /**
   * Navigate to parent route, handling fragments
   */
  private navigateToParent(parentRoute: string): void {
    if (parentRoute.includes('#')) {
      const [path, fragment] = parentRoute.split('#');
      this.router.navigate([path || '/'], { fragment }).then(() => {
        // Handle scroll for hero section manually
        if (fragment === 'hero') {
          setTimeout(() => {
            window.scrollTo({ top: 0, behavior: 'auto' });
          }, 150);
        }
      });
    } else {
      this.router.navigate([parentRoute]);
    }
  }

  /**
   * Clean URL by removing query params and fragments for matching
   */
  private cleanUrl(url: string): string {
    return url.split('?')[0].split('#')[0];
  }
}
