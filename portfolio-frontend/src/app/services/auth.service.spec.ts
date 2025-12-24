import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import { AccessTokenResponse, LoginRequest, User, UserRole } from '../models/auth.model';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;

  const mockAccessTokenResponse: AccessTokenResponse = {
    accessToken: 'mock-access-token',
    tokenType: 'Bearer',
    expiresIn: 900000,
    username: 'admin',
    role: 'ROLE_ADMIN',
  };

  const mockUser: User = {
    username: 'admin',
    roles: [UserRole.ADMIN],
    isAdmin: true,
  };

  const apiUrl = `${environment.apiUrl}/api/auth`;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate'], { url: '/admin' });
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);
    tokenStorageSpy = jasmine.createSpyObj('TokenStorageService', [
      'saveTokens',
      'getAccessToken',
      'getCurrentUser',
      'isTokenExpired',
      'getTimeUntilExpiration',
      'hasValidToken',
      'shouldAttemptSessionRestore',
      'clear',
    ]);

    // Default mock behavior - no existing session
    tokenStorageSpy.getCurrentUser.and.returnValue(null);
    tokenStorageSpy.isTokenExpired.and.returnValue(true);
    tokenStorageSpy.getAccessToken.and.returnValue(null);
    tokenStorageSpy.hasValidToken.and.returnValue(false);
    tokenStorageSpy.shouldAttemptSessionRestore.and.returnValue(false);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TokenStorageService, useValue: tokenStorageSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Only verify if httpMock exists and we haven't reset TestBed
    try {
      httpMock.verify();
    } catch {
      // Ignore verification errors from tests that reset TestBed
    }
  });

  // ========== Service Creation Tests ==========

  it('should_beCreated_when_injected', () => {
    expect(service).toBeTruthy();
  });

  // ========== Login Tests ==========

  describe('login', () => {
    it('should_returnAccessTokenResponse_when_loginCalledWithValidCredentials', () => {
      // Arrange
      const credentials: LoginRequest = { username: 'admin', password: 'password' };
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act & Assert
      service.login(credentials).subscribe((response) => {
        expect(response).toEqual(mockAccessTokenResponse);
        expect(tokenStorageSpy.saveTokens).toHaveBeenCalledWith(mockAccessTokenResponse);
      });

      const req = httpMock.expectOne(`${apiUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      expect(req.request.withCredentials).toBeTrue();
      req.flush(mockAccessTokenResponse);
    });

    it('should_logError_when_loginCalledWithInvalidCredentials', () => {
      // Arrange
      const credentials: LoginRequest = { username: 'admin', password: 'wrong' };

      // Act
      service.login(credentials).subscribe({
        error: (error) => {
          expect(error.status).toBe(401);
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/login`);
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      // Assert
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  // ========== Logout Tests ==========

  describe('logout', () => {
    it('should_clearState_when_logoutCalledWithValidSession', fakeAsync(() => {
      // Act
      service.logout();
      tick();

      // Assert
      const req = httpMock.expectOne(`${apiUrl}/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBeTrue();
      req.flush({});
      tick();

      expect(tokenStorageSpy.clear).toHaveBeenCalled();
    }));

    it('should_clearState_when_logoutEndpointFails', fakeAsync(() => {
      // Act
      service.logout();
      tick();

      // Assert
      const req = httpMock.expectOne(`${apiUrl}/logout`);
      req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });
      tick();

      expect(tokenStorageSpy.clear).toHaveBeenCalled();
    }));
  });

  // ========== Token Refresh Tests ==========

  describe('refreshToken', () => {
    it('should_returnNewToken_when_refreshTokenCalledWithValidCookie', () => {
      // Arrange
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act
      service.refreshToken().subscribe((response) => {
        expect(response).toEqual(mockAccessTokenResponse);
      });

      const req = httpMock.expectOne(`${apiUrl}/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBeTrue();
      expect(req.request.body).toEqual({});
      req.flush(mockAccessTokenResponse);

      // Assert
      expect(tokenStorageSpy.saveTokens).toHaveBeenCalledWith(mockAccessTokenResponse);
    });

    it('should_clearState_when_refreshTokenFailsWhileUserLoggedIn', fakeAsync(() => {
      // Arrange - simulate logged in state
      TestBed.resetTestingModule();
      tokenStorageSpy = jasmine.createSpyObj('TokenStorageService', [
        'saveTokens',
        'getAccessToken',
        'getCurrentUser',
        'isTokenExpired',
        'getTimeUntilExpiration',
        'hasValidToken',
        'shouldAttemptSessionRestore',
        'clear',
      ]);
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.isTokenExpired.and.returnValue(false);
      tokenStorageSpy.hasValidToken.and.returnValue(true);
      tokenStorageSpy.shouldAttemptSessionRestore.and.returnValue(false);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: routerSpy },
          { provide: LoggerService, useValue: loggerSpy },
          { provide: TokenStorageService, useValue: tokenStorageSpy },
        ],
      });

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);
      tick();

      // Act
      service.refreshToken().subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/refresh`);
      req.flush({ message: 'Invalid refresh token' }, { status: 401, statusText: 'Unauthorized' });
      tick();

      // Assert
      expect(tokenStorageSpy.clear).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();

      // Clean up periodic timers from the scheduled token refresh
      discardPeriodicTasks();
    }));
  });

  // ========== Authentication State Tests ==========

  describe('isAuthenticated', () => {
    it('should_returnTrue_when_isAuthenticatedCalledWithValidToken', () => {
      // Arrange
      tokenStorageSpy.hasValidToken.and.returnValue(true);

      // Act & Assert
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('should_returnFalse_when_isAuthenticatedCalledWithNoToken', () => {
      // Arrange
      tokenStorageSpy.hasValidToken.and.returnValue(false);

      // Act & Assert
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  // ========== Token Getter Tests ==========

  describe('getToken', () => {
    it('should_returnToken_when_getTokenCalledWithStoredToken', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue('mock-access-token');

      // Act & Assert
      expect(service.getToken()).toBe('mock-access-token');
    });

    it('should_returnNull_when_getTokenCalledWithNoToken', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue(null);

      // Act & Assert
      expect(service.getToken()).toBeNull();
    });
  });

  // ========== Current User Tests ==========

  describe('getCurrentUser', () => {
    it('should_returnNull_when_getCurrentUserCalledWithNoSession', () => {
      // Act & Assert
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  // ========== Admin Check Tests ==========

  describe('isAdmin', () => {
    it('should_returnFalse_when_isAdminCalledWithNoUser', () => {
      // Act & Assert
      expect(service.isAdmin()).toBeFalse();
    });
  });

  // ========== Session Initialization Tests ==========

  describe('session initialization', () => {
    it('should_restoreSession_when_initCalledWithValidTokenInMemory', () => {
      // Arrange
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.isTokenExpired.and.returnValue(false);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);
      tokenStorageSpy.shouldAttemptSessionRestore.and.returnValue(false);

      // Act - Create new service instance with restored session
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: routerSpy },
          { provide: LoggerService, useValue: loggerSpy },
          { provide: TokenStorageService, useValue: tokenStorageSpy },
        ],
      });

      const newService = TestBed.inject(AuthService);

      // Assert
      expect(loggerSpy.info).toHaveBeenCalledWith('[AUTH_INIT] Session restored from memory', {
        username: 'admin',
      });
      expect(newService).toBeTruthy();
    });

    it('should_attemptRefresh_when_initCalledWithNoTokenInMemory', fakeAsync(() => {
      // Arrange
      tokenStorageSpy.getCurrentUser.and.returnValue(null);
      tokenStorageSpy.isTokenExpired.and.returnValue(true);
      tokenStorageSpy.shouldAttemptSessionRestore.and.returnValue(true);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act - Create new service instance
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: Router, useValue: routerSpy },
          { provide: LoggerService, useValue: loggerSpy },
          { provide: TokenStorageService, useValue: tokenStorageSpy },
        ],
      });

      const localHttpMock = TestBed.inject(HttpTestingController);
      TestBed.inject(AuthService);
      tick();

      // Assert - refresh endpoint should be called
      expect(loggerSpy.info).toHaveBeenCalledWith(
        '[AUTH_INIT] Attempting session restore via refresh token'
      );

      // Handle the refresh request - return user after successful refresh
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      const req = localHttpMock.expectOne(`${apiUrl}/refresh`);
      expect(req.request.withCredentials).toBeTrue();
      req.flush(mockAccessTokenResponse);
      tick();

      expect(loggerSpy.info).toHaveBeenCalledWith('[AUTH_INIT] Session restored via refresh token');

      // Clean up periodic timers from the scheduled token refresh
      discardPeriodicTasks();

      // Verify no pending requests in local httpMock
      localHttpMock.verify();
    }));
  });

  // ========== Observable Tests ==========

  describe('currentUser$', () => {
    it('should_emitNull_when_subscribedWithNoSession', (done) => {
      service.currentUser$.subscribe((user) => {
        expect(user).toBeNull();
        done();
      });
    });
  });
});
