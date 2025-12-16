import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActiveUsersService } from './active-users.service';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';

describe('ActiveUsersService', () => {
  let service: ActiveUsersService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let tokenStorageSpy: jasmine.SpyObj<TokenStorageService>;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'debug', 'warn', 'error']);
    tokenStorageSpy = jasmine.createSpyObj('TokenStorageService', ['getAccessToken']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ActiveUsersService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TokenStorageService, useValue: tokenStorageSpy },
      ],
    });

    service = TestBed.inject(ActiveUsersService);
  });

  afterEach(() => {
    service.disconnect();
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should_returnZero_when_getCurrentCountCalledInitially', () => {
    // Act
    const count = service.getCurrentCount();

    // Assert
    expect(count).toBe(0);
  });

  it('should_haveActiveUsersObservable', () => {
    // Assert
    expect(service.activeUsers$).toBeDefined();
  });

  it('should_haveConnectedObservable', () => {
    // Assert
    expect(service.connected$).toBeDefined();
  });

  // ========== connect Tests ==========

  it('should_logWarning_when_connectCalledWithoutToken', () => {
    // Arrange
    tokenStorageSpy.getAccessToken.and.returnValue(null);

    // Act
    service.connect();

    // Assert
    expect(loggerSpy.warn).toHaveBeenCalledWith('[ACTIVE_USERS] No access token available');
  });

  it('should_notConnect_when_connectCalledWithoutToken', () => {
    // Arrange
    tokenStorageSpy.getAccessToken.and.returnValue(null);
    const fetchSpy = spyOn(window, 'fetch');

    // Act
    service.connect();

    // Assert
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('should_logInfo_when_connectCalledWithToken', () => {
    // Arrange
    tokenStorageSpy.getAccessToken.and.returnValue('valid-token');
    spyOn(window, 'fetch').and.returnValue(
      new Promise(() => {}) // Never resolves to avoid async issues
    );

    // Act
    service.connect();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[ACTIVE_USERS] Connecting to SSE stream');
  });

  it('should_callFetchWithCorrectUrl_when_connectCalled', () => {
    // Arrange
    tokenStorageSpy.getAccessToken.and.returnValue('valid-token');
    const fetchSpy = spyOn(window, 'fetch').and.returnValue(
      new Promise(() => {}) // Never resolves to avoid async issues
    );

    // Act
    service.connect();

    // Assert
    expect(fetchSpy).toHaveBeenCalledWith(
      jasmine.stringMatching(/\/admin\/visitors\/stream$/),
      jasmine.objectContaining({
        method: 'GET',
        headers: jasmine.objectContaining({
          Authorization: 'Bearer valid-token',
          Accept: 'text/event-stream',
        }),
      })
    );
  });

  // ========== disconnect Tests ==========

  it('should_logInfo_when_disconnectCalled', () => {
    // Act
    service.disconnect();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[ACTIVE_USERS] Disconnected');
  });
});
