import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { ActivityMonitorService } from './activity-monitor.service';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { LoggerService } from './logger.service';
import { AccessTokenResponse } from '../models/auth.model';
import { of, throwError } from 'rxjs';

describe('ActivityMonitorService', () => {
  let service: ActivityMonitorService;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const mockAccessTokenResponse: AccessTokenResponse = {
    accessToken: 'new-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    username: 'admin',
    role: 'ROLE_ADMIN',
  };

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'refreshToken']);
    tokenStorageSpy = jasmine.createSpyObj('TokenStorageService', ['getTimeUntilExpiration']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'debug', 'warn', 'error']);

    TestBed.configureTestingModule({
      providers: [
        ActivityMonitorService,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: TokenStorageService, useValue: tokenStorageSpy },
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(ActivityMonitorService);
  });

  afterEach(() => {
    service.stopMonitoring();
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== startMonitoring Tests ==========

  it('should_logInfo_when_startMonitoringCalled', () => {
    // Arrange / Act
    service.startMonitoring();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Started monitoring user activity'
    );
  });

  it('should_logDebug_when_startMonitoringCalledWhileAlreadyMonitoring', () => {
    // Arrange
    service.startMonitoring();
    loggerSpy.debug.calls.reset();

    // Act
    service.startMonitoring();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Already monitoring user activity'
    );
  });

  it('should_notStartTwice_when_startMonitoringCalledMultipleTimes', () => {
    // Arrange / Act
    service.startMonitoring();
    service.startMonitoring();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledTimes(1);
  });

  // ========== stopMonitoring Tests ==========

  it('should_logInfo_when_stopMonitoringCalledWhileMonitoring', () => {
    // Arrange
    service.startMonitoring();

    // Act
    service.stopMonitoring();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Stopped monitoring user activity'
    );
  });

  it('should_doNothing_when_stopMonitoringCalledWhileNotMonitoring', () => {
    // Arrange
    loggerSpy.info.calls.reset();

    // Act
    service.stopMonitoring();

    // Assert
    expect(loggerSpy.info).not.toHaveBeenCalled();
  });

  // ========== Activity Detection Tests ==========

  it('should_notRefreshToken_when_userNotAuthenticated', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(false);
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();

    discardPeriodicTasks();
  }));

  it('should_notRefreshToken_when_tokenNotExpiringSoon', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(600000); // 10 minutes
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();

    discardPeriodicTasks();
  }));

  it('should_refreshToken_when_userActiveAndTokenExpiringSoon', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000); // 4 minutes
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).toHaveBeenCalled();
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] User active and token expires soon, refreshing',
      jasmine.objectContaining({ expiresInMs: 240000 })
    );

    discardPeriodicTasks();
  }));

  it('should_logSuccess_when_tokenRefreshedSuccessfully', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Token refreshed successfully due to activity'
    );

    discardPeriodicTasks();
  }));

  it('should_logError_when_tokenRefreshFails', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh failed')));
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(loggerSpy.error).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Failed to refresh token on activity',
      jasmine.objectContaining({ error: 'Refresh failed' })
    );

    discardPeriodicTasks();
  }));

  it('should_skipRefresh_when_refreshTriggeredRecently', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();

    // Act - First activity triggers refresh
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Act - Second activity too soon (30 seconds later, need 60 seconds minimum)
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert - Only one refresh call
    expect(authServiceSpy.refreshToken).toHaveBeenCalledTimes(1);
    expect(loggerSpy.debug).toHaveBeenCalledWith(
      '[ACTIVITY_MONITOR] Token expires soon but refresh triggered recently, skipping',
      jasmine.any(Object)
    );

    discardPeriodicTasks();
  }));

  it('should_notRefreshToken_when_tokenAlreadyExpired', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(-1000); // Already expired
    service.startMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();

    discardPeriodicTasks();
  }));

  // ========== Event Listener Tests ==========

  it('should_detectKeypress_when_keypressEventFired', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();

    // Act
    document.dispatchEvent(new KeyboardEvent('keypress', { key: 'a' }));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).toHaveBeenCalled();

    discardPeriodicTasks();
  }));

  it('should_detectScroll_when_scrollEventFired', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();

    // Act
    document.dispatchEvent(new Event('scroll'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).toHaveBeenCalled();

    discardPeriodicTasks();
  }));

  // ========== Cleanup Tests ==========

  it('should_stopListeningToEvents_when_stopMonitoringCalled', fakeAsync(() => {
    // Arrange
    authServiceSpy.isAuthenticated.and.returnValue(true);
    tokenStorageSpy.getTimeUntilExpiration.and.returnValue(240000);
    authServiceSpy.refreshToken.and.returnValue(of(mockAccessTokenResponse));
    service.startMonitoring();
    service.stopMonitoring();

    // Act
    document.dispatchEvent(new MouseEvent('click'));
    tick(35000);

    // Assert
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();

    discardPeriodicTasks();
  }));
});
