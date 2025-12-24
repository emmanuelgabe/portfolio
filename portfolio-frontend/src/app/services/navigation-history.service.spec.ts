import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';
import { Subject } from 'rxjs';
import { NavigationHistoryService } from './navigation-history.service';
import { LoggerService } from './logger.service';

describe('NavigationHistoryService', () => {
  let service: NavigationHistoryService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let routerEvents$: Subject<NavigationEnd>;

  beforeEach(() => {
    routerEvents$ = new Subject<NavigationEnd>();
    const loggerSpyObj = jasmine.createSpyObj('LoggerService', ['info', 'debug', 'error', 'warn']);
    const routerSpyObj = jasmine.createSpyObj('Router', ['navigate'], {
      url: '/',
      events: routerEvents$.asObservable(),
    });
    const locationSpyObj = jasmine.createSpyObj('Location', ['path']);
    locationSpyObj.path.and.returnValue('/');

    TestBed.configureTestingModule({
      providers: [
        NavigationHistoryService,
        { provide: LoggerService, useValue: loggerSpyObj },
        { provide: Router, useValue: routerSpyObj },
        { provide: Location, useValue: locationSpyObj },
        { provide: PLATFORM_ID, useValue: 'browser' },
      ],
    });

    service = TestBed.inject(NavigationHistoryService);
    loggerSpy = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    service.ngOnDestroy();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== getParentRoute Tests ==========

  it('should return /#hero for project detail route', () => {
    // Arrange
    const url = '/projects/123';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/#hero');
  });

  it('should return /blog for article detail route', () => {
    // Arrange
    const url = '/blog/my-article-slug';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/blog');
  });

  it('should return /#hero for blog list route', () => {
    // Arrange
    const url = '/blog';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/#hero');
  });

  it('should return / for contact route', () => {
    // Arrange
    const url = '/contact';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/');
  });

  it('should return / for login route', () => {
    // Arrange
    const url = '/login';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/');
  });

  it('should return /admin for admin child routes', () => {
    // Arrange
    const url = '/admin/projects/new';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/admin');
  });

  it('should return /admindemo for demo child routes', () => {
    // Arrange
    const url = '/admindemo/articles';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/admindemo');
  });

  it('should return null for home route', () => {
    // Arrange
    const url = '/';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBeNull();
  });

  it('should return null for empty route', () => {
    // Arrange
    const url = '';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBeNull();
  });

  it('should handle route with query params', () => {
    // Arrange
    const url = '/blog/my-article?ref=home';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/blog');
  });

  it('should handle route with fragment', () => {
    // Arrange
    const url = '/projects/123#details';

    // Act
    const result = service.getParentRoute(url);

    // Assert
    expect(result).toBe('/#hero');
  });

  // ========== hasInternalHistory Tests ==========

  it('should return false when stack has only initial route', () => {
    // Arrange
    service.initialize();

    // Act
    const result = service.hasInternalHistory();

    // Assert
    expect(result).toBe(false);
  });

  it('should return true when stack has multiple routes', fakeAsync(() => {
    // Arrange
    service.initialize();

    // Act
    routerEvents$.next(new NavigationEnd(1, '/blog', '/blog'));
    tick();

    // Assert
    expect(service.hasInternalHistory()).toBe(true);
  }));

  // ========== initialize Tests ==========

  it('should log initialization', () => {
    // Act
    service.initialize();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[NAVIGATION] Navigation history service initialized'
    );
  });

  it('should not initialize twice', () => {
    // Arrange
    service.initialize();
    loggerSpy.info.calls.reset();

    // Act
    service.initialize();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith('[NAVIGATION] Already initialized');
  });

  // ========== Navigation Tracking Tests ==========

  it('should track navigation events', fakeAsync(() => {
    // Arrange
    service.initialize();

    // Act
    routerEvents$.next(new NavigationEnd(1, '/blog', '/blog'));
    tick();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith(
      '[NAVIGATION] Route added to stack',
      jasmine.objectContaining({ url: '/blog' })
    );
  }));

  it('should not add duplicate routes to stack', fakeAsync(() => {
    // Arrange
    service.initialize();
    loggerSpy.debug.calls.reset();

    // Act
    routerEvents$.next(new NavigationEnd(1, '/', '/'));
    tick();

    // Assert
    const addCalls = loggerSpy.debug.calls
      .allArgs()
      .filter((args) => args[0] === '[NAVIGATION] Route added to stack');
    expect(addCalls.length).toBe(0);
  }));
});
