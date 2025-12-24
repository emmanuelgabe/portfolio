import { TestBed } from '@angular/core/testing';
import { WebVitalsService } from './web-vitals.service';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

describe('WebVitalsService', () => {
  let service: WebVitalsService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let originalWebVitalsEnabled: boolean;

  beforeEach(() => {
    originalWebVitalsEnabled = environment.webVitals.enabled;

    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [WebVitalsService, { provide: LoggerService, useValue: loggerSpy }],
    });

    service = TestBed.inject(WebVitalsService);
  });

  afterEach(() => {
    environment.webVitals.enabled = originalWebVitalsEnabled;
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should_logInitialization_when_initCalledWithWebVitalsEnabled', () => {
    // Arrange
    environment.webVitals.enabled = true;

    // Act
    service.init();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[WEB_VITALS] Initializing Web Vitals collection');
  });

  it('should_notLogInitialization_when_initCalledWithWebVitalsDisabled', () => {
    // Arrange
    environment.webVitals.enabled = false;

    // Act
    service.init();

    // Assert
    expect(loggerSpy.info).not.toHaveBeenCalled();
  });

  it('should_haveInitMethod_when_serviceCreated', () => {
    // Assert
    expect(typeof service.init).toBe('function');
  });

  it('should_notThrow_when_initCalledMultipleTimes', () => {
    // Arrange
    environment.webVitals.enabled = true;

    // Act / Assert
    expect(() => {
      service.init();
      service.init();
    }).not.toThrow();
  });

  it('should_handleDisabledState_when_webVitalsDisabled', () => {
    // Arrange
    environment.webVitals.enabled = false;

    // Act
    service.init();

    // Assert - should complete without errors
    expect(service).toBeTruthy();
  });
});
