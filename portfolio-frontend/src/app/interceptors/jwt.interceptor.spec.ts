import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';
import { LoggerService } from '../services/logger.service';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthResponse } from '../models/auth.model';
import { of, throwError } from 'rxjs';

describe('jwtInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;

  const mockAuthResponse: AuthResponse = {
    accessToken: 'new-access-token',
    refreshToken: 'new-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900000,
  };

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken', 'refreshToken', 'logout']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['debug', 'info', 'warn', 'error']);
    tokenStorageSpy = jasmine.createSpyObj('TokenStorageService', ['isTokenExpired']);

    // Default mock behavior
    authServiceSpy.getToken.and.returnValue(null);
    tokenStorageSpy.isTokenExpired.and.returnValue(false);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TokenStorageService, useValue: tokenStorageSpy },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ========== Authorization Header Tests ==========

  describe('Authorization header', () => {
    it('should add Authorization header when token exists', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-access-token');

      // Act
      httpClient.get('/api/projects').subscribe();

      // Assert
      const req = httpMock.expectOne('/api/projects');
      expect(req.request.headers.has('Authorization')).toBeTrue();
      expect(req.request.headers.get('Authorization')).toBe('Bearer mock-access-token');
      req.flush([]);
    });

    it('should not add Authorization header when no token', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue(null);

      // Act
      httpClient.get('/api/projects').subscribe();

      // Assert
      const req = httpMock.expectOne('/api/projects');
      expect(req.request.headers.has('Authorization')).toBeFalse();
      req.flush([]);
    });

    it('should not add Authorization header to auth endpoints', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-access-token');

      // Act
      httpClient.post('/api/auth/login', {}).subscribe();

      // Assert
      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.headers.has('Authorization')).toBeFalse();
      req.flush({});
    });

    it('should not add Authorization header to refresh endpoint', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-access-token');

      // Act
      httpClient.post('/api/auth/refresh', {}).subscribe();

      // Assert
      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.headers.has('Authorization')).toBeFalse();
      req.flush({});
    });
  });

  // ========== 401 Error Handling Tests ==========

  describe('401 error handling', () => {
    it('should attempt token refresh on 401 error', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValues('old-token', 'new-token');
      authServiceSpy.refreshToken.and.returnValue(of(mockAuthResponse));

      // Act
      httpClient.get('/api/admin/projects').subscribe();

      // First request fails with 401
      const req = httpMock.expectOne('/api/admin/projects');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      // After refresh, retry request
      const retryReq = httpMock.expectOne('/api/admin/projects');
      expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-token');
      retryReq.flush([]);

      // Assert
      expect(authServiceSpy.refreshToken).toHaveBeenCalled();
    });

    it('should logout when token refresh fails on 401', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('expired-token');
      authServiceSpy.refreshToken.and.returnValue(
        throwError(() => ({ status: 401, message: 'Invalid refresh token' }))
      );

      // Act
      httpClient.get('/api/admin/projects').subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne('/api/admin/projects');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      // Assert
      expect(authServiceSpy.logout).toHaveBeenCalled();
    });

    it('should not attempt refresh for auth endpoints on 401', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-token');

      // Act
      httpClient.post('/api/auth/login', {}).subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush({ message: 'Bad credentials' }, { status: 401, statusText: 'Unauthorized' });

      // Assert
      expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    });
  });

  // ========== 403 Error Handling Tests ==========

  describe('403 error handling', () => {
    it('should attempt token refresh on 403 when token is expired', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValues('expired-token', 'new-token');
      tokenStorageSpy.isTokenExpired.and.returnValue(true);
      authServiceSpy.refreshToken.and.returnValue(of(mockAuthResponse));

      // Act
      httpClient.get('/api/admin/projects').subscribe();

      // First request fails with 403
      const req = httpMock.expectOne('/api/admin/projects');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      // After refresh, retry request
      const retryReq = httpMock.expectOne('/api/admin/projects');
      expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-token');
      retryReq.flush([]);

      // Assert
      expect(authServiceSpy.refreshToken).toHaveBeenCalled();
    });

    it('should not attempt refresh on 403 when token is not expired', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('valid-token');
      tokenStorageSpy.isTokenExpired.and.returnValue(false);

      // Act
      httpClient.get('/api/admin/restricted').subscribe({
        error: () => {
          // Expected error - genuine permission issue
        },
      });

      const req = httpMock.expectOne('/api/admin/restricted');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      // Assert
      expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    });

    it('should not attempt refresh for auth endpoints on 403', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-token');
      tokenStorageSpy.isTokenExpired.and.returnValue(true);

      // Act
      httpClient.post('/api/auth/logout', {}).subscribe({
        error: () => {
          // Expected error
        },
      });

      const req = httpMock.expectOne('/api/auth/logout');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      // Assert
      expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    });
  });

  // ========== Other Error Handling Tests ==========

  describe('other errors', () => {
    it('should pass through 404 errors without refresh attempt', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('valid-token');

      // Act
      httpClient.get('/api/projects/999').subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne('/api/projects/999');
      req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

      // Assert
      expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    });

    it('should pass through 500 errors without refresh attempt', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('valid-token');

      // Act
      httpClient.get('/api/projects').subscribe({
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne('/api/projects');
      req.flush({ message: 'Server error' }, { status: 500, statusText: 'Internal Server Error' });

      // Assert
      expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    });
  });

  // ========== Logging Tests ==========

  describe('logging', () => {
    it('should log debug when adding Authorization header', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValue('mock-token');

      // Act
      httpClient.get('/api/projects').subscribe();

      const req = httpMock.expectOne('/api/projects');
      req.flush([]);

      // Assert
      expect(loggerSpy.debug).toHaveBeenCalledWith(
        '[JWT_INTERCEPTOR] Added Authorization header',
        jasmine.objectContaining({
          method: 'GET',
          url: '/api/projects',
        })
      );
    });

    it('should log warning on 401 error', () => {
      // Arrange
      authServiceSpy.getToken.and.returnValues('old-token', 'new-token');
      authServiceSpy.refreshToken.and.returnValue(of(mockAuthResponse));

      // Act
      httpClient.get('/api/admin/projects').subscribe();

      const req = httpMock.expectOne('/api/admin/projects');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      const retryReq = httpMock.expectOne('/api/admin/projects');
      retryReq.flush([]);

      // Assert
      expect(loggerSpy.warn).toHaveBeenCalledWith(
        '[JWT_INTERCEPTOR] 401 Unauthorized, attempting token refresh',
        jasmine.objectContaining({
          url: '/api/admin/projects',
        })
      );
    });
  });
});
