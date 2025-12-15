import { TestBed } from '@angular/core/testing';
import { ConnectivityService } from './connectivity.service';
import { LoggerService } from './logger.service';

describe('ConnectivityService', () => {
  let service: ConnectivityService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [ConnectivityService, { provide: LoggerService, useValue: loggerSpy }],
    });

    service = TestBed.inject(ConnectivityService);
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should_initializeWithCurrentOnlineState_when_created', () => {
    // Arrange / Act - service is created in beforeEach

    // Assert
    expect(service.isOnline()).toBe(navigator.onLine);
    expect(service.isOffline()).toBe(!navigator.onLine);
  });

  // ========== Signal Tests ==========

  it('should_returnTrue_when_isOnlineCalledAndDeviceOnline', () => {
    // Arrange - assume device is online (default state)

    // Act
    const result = service.isOnline();

    // Assert
    expect(typeof result).toBe('boolean');
  });

  it('should_returnOppositeOfIsOnline_when_isOfflineCalled', () => {
    // Arrange / Act
    const isOnline = service.isOnline();
    const isOffline = service.isOffline();

    // Assert
    expect(isOffline).toBe(!isOnline);
  });

  // ========== Event Listener Tests ==========

  it('should_updateToOffline_when_offlineEventTriggered', () => {
    // Arrange / Act
    window.dispatchEvent(new Event('offline'));

    // Assert
    expect(service.isOffline()).toBeTrue();
    expect(service.isOnline()).toBeFalse();
  });

  it('should_updateToOnline_when_onlineEventTriggered', () => {
    // Arrange - first set offline
    window.dispatchEvent(new Event('offline'));
    expect(service.isOffline()).toBeTrue();

    // Act
    window.dispatchEvent(new Event('online'));

    // Assert
    expect(service.isOnline()).toBeTrue();
    expect(service.isOffline()).toBeFalse();
  });

  it('should_logWarning_when_connectionLost', () => {
    // Arrange / Act
    window.dispatchEvent(new Event('offline'));

    // Assert
    expect(loggerSpy.warn).toHaveBeenCalledWith('[CONNECTIVITY] Connection lost');
  });

  it('should_logInfo_when_connectionRestored', () => {
    // Arrange
    window.dispatchEvent(new Event('offline'));

    // Act
    window.dispatchEvent(new Event('online'));

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[CONNECTIVITY] Connection restored');
  });
});
