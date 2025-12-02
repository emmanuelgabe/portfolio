import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import { AuthResponse, LoginRequest, User, UserRole } from '../models/auth.model';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;

  const mockAuthResponse: AuthResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900000,
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
      'getRefreshToken',
      'getCurrentUser',
      'isTokenExpired',
      'getTimeUntilExpiration',
      'isRememberMeEnabled',
      'clear',
    ]);

    // Default mock behavior - no existing session
    tokenStorageSpy.getCurrentUser.and.returnValue(null);
    tokenStorageSpy.isTokenExpired.and.returnValue(true);
    tokenStorageSpy.getAccessToken.and.returnValue(null);
    tokenStorageSpy.getRefreshToken.and.returnValue(null);

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
    httpMock.verify();
  });

  // ========== Service Creation Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== Login Tests ==========

  describe('login', () => {
    it('should return AuthResponse on successful login', () => {
      // Arrange
      const credentials: LoginRequest = { username: 'admin', password: 'password' };
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act & Assert
      service.login(credentials, false).subscribe((response) => {
        expect(response).toEqual(mockAuthResponse);
        expect(tokenStorageSpy.saveTokens).toHaveBeenCalledWith(mockAuthResponse, false);
      });

      const req = httpMock.expectOne(`${apiUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(mockAuthResponse);
    });

    it('should save tokens with rememberMe when provided', () => {
      // Arrange
      const credentials: LoginRequest = { username: 'admin', password: 'password' };
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act
      service.login(credentials, true).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/login`);
      req.flush(mockAuthResponse);

      // Assert
      expect(tokenStorageSpy.saveTokens).toHaveBeenCalledWith(mockAuthResponse, true);
    });

    it('should log error on failed login', () => {
      // Arrange
      const credentials: LoginRequest = { username: 'admin', password: 'wrong' };

      // Act
      service.login(credentials, false).subscribe({
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
    it('should call logout endpoint and clear state when refresh token exists', fakeAsync(() => {
      // Arrange
      tokenStorageSpy.getRefreshToken.and.returnValue('mock-refresh-token');

      // Act
      service.logout();
      tick();

      // Assert
      const req = httpMock.expectOne(`${apiUrl}/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'mock-refresh-token' });
      req.flush({});
      tick();

      expect(tokenStorageSpy.clear).toHaveBeenCalled();
    }));

    it('should clear state locally when no refresh token', () => {
      // Arrange
      tokenStorageSpy.getRefreshToken.and.returnValue(null);

      // Act
      service.logout();

      // Assert
      expect(tokenStorageSpy.clear).toHaveBeenCalled();
      expect(loggerSpy.info).toHaveBeenCalledWith('[AUTH_LOGOUT] Local logout (no refresh token)');
    });
  });

  // ========== Token Refresh Tests ==========

  describe('refreshToken', () => {
    it('should refresh token successfully', () => {
      // Arrange
      tokenStorageSpy.getRefreshToken.and.returnValue('mock-refresh-token');
      tokenStorageSpy.isRememberMeEnabled.and.returnValue(false);
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

      // Act
      service.refreshToken().subscribe((response) => {
        expect(response).toEqual(mockAuthResponse);
      });

      const req = httpMock.expectOne(`${apiUrl}/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'mock-refresh-token' });
      req.flush(mockAuthResponse);

      // Assert
      expect(tokenStorageSpy.saveTokens).toHaveBeenCalled();
    });

    it('should return error when no refresh token available', () => {
      // Arrange
      tokenStorageSpy.getRefreshToken.and.returnValue(null);

      // Act
      service.refreshToken().subscribe({
        error: (error) => {
          expect(error.message).toBe('No refresh token available');
        },
      });

      // Assert
      expect(tokenStorageSpy.clear).toHaveBeenCalled();
    });

    it('should clear state on refresh failure', () => {
      // Arrange
      tokenStorageSpy.getRefreshToken.and.returnValue('mock-refresh-token');

      // Act
      service.refreshToken().subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/refresh`);
      req.flush({ message: 'Invalid refresh token' }, { status: 401, statusText: 'Unauthorized' });

      // Assert
      expect(tokenStorageSpy.clear).toHaveBeenCalled();
      expect(loggerSpy.error).toHaveBeenCalled();
    });
  });

  // ========== Authentication State Tests ==========

  describe('isAuthenticated', () => {
    it('should return true when valid token exists', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue('valid-token');
      tokenStorageSpy.isTokenExpired.and.returnValue(false);

      // Act & Assert
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('should return false when no token exists', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue(null);

      // Act & Assert
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should return false when token is expired', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue('expired-token');
      tokenStorageSpy.isTokenExpired.and.returnValue(true);

      // Act & Assert
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  // ========== Token Getter Tests ==========

  describe('getToken', () => {
    it('should return access token from storage', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue('mock-access-token');

      // Act & Assert
      expect(service.getToken()).toBe('mock-access-token');
    });

    it('should return null when no token', () => {
      // Arrange
      tokenStorageSpy.getAccessToken.and.returnValue(null);

      // Act & Assert
      expect(service.getToken()).toBeNull();
    });
  });

  // ========== Current User Tests ==========

  describe('getCurrentUser', () => {
    it('should return null when no user is logged in', () => {
      // Act & Assert
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  // ========== Admin Check Tests ==========

  describe('isAdmin', () => {
    it('should return false when no user is logged in', () => {
      // Act & Assert
      expect(service.isAdmin()).toBeFalse();
    });
  });

  // ========== Session Initialization Tests ==========

  describe('session initialization', () => {
    it('should restore session when valid token exists on creation', () => {
      // Arrange
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.isTokenExpired.and.returnValue(false);
      tokenStorageSpy.getTimeUntilExpiration.and.returnValue(900000);

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
      expect(loggerSpy.info).toHaveBeenCalledWith('[AUTH_INIT] Session restored', {
        username: 'admin',
      });
      expect(newService).toBeTruthy();
    });

    it('should clear expired session on creation', () => {
      // Arrange
      tokenStorageSpy.getCurrentUser.and.returnValue(mockUser);
      tokenStorageSpy.isTokenExpired.and.returnValue(true);

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

      TestBed.inject(AuthService);

      // Assert
      expect(tokenStorageSpy.clear).toHaveBeenCalled();
      expect(loggerSpy.info).toHaveBeenCalledWith('[AUTH_INIT] Session expired, clearing tokens');
    });
  });

  // ========== Observable Tests ==========

  describe('currentUser$', () => {
    it('should emit null initially when no session', (done) => {
      service.currentUser$.subscribe((user) => {
        expect(user).toBeNull();
        done();
      });
    });
  });
});
