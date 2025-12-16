/* eslint-disable no-console -- Test file for LoggerService requires console method spying */
import { TestBed } from '@angular/core/testing';
import { LoggerService, LogLevel } from './logger.service';

describe('LoggerService', () => {
  let service: LoggerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LoggerService],
    });

    service = TestBed.inject(LoggerService);
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== LogLevel Enum Tests ==========

  it('should_haveTRACEAsLowestLevel_when_logLevelEnumChecked', () => {
    expect(LogLevel.TRACE).toBe(0);
  });

  it('should_haveFATALAsHighestLevel_when_logLevelEnumChecked', () => {
    expect(LogLevel.FATAL).toBe(5);
  });

  it('should_haveCorrectLevelOrder_when_logLevelEnumChecked', () => {
    expect(LogLevel.TRACE).toBeLessThan(LogLevel.DEBUG);
    expect(LogLevel.DEBUG).toBeLessThan(LogLevel.INFO);
    expect(LogLevel.INFO).toBeLessThan(LogLevel.WARN);
    expect(LogLevel.WARN).toBeLessThan(LogLevel.ERROR);
    expect(LogLevel.ERROR).toBeLessThan(LogLevel.FATAL);
  });

  // ========== Console Output Tests ==========

  it('should_logToConsoleInfo_when_infoCalled', () => {
    // Arrange
    spyOn(console, 'info');

    // Act
    service.info('[TEST] Info message');

    // Assert
    expect(console.info).toHaveBeenCalled();
  });

  it('should_logToConsoleWarn_when_warnCalled', () => {
    // Arrange
    spyOn(console, 'warn');

    // Act
    service.warn('[TEST] Warning message');

    // Assert
    expect(console.warn).toHaveBeenCalled();
  });

  it('should_logToConsoleError_when_errorCalled', () => {
    // Arrange
    spyOn(console, 'error');

    // Act
    service.error('[TEST] Error message');

    // Assert
    expect(console.error).toHaveBeenCalled();
  });

  it('should_logToConsoleError_when_fatalCalled', () => {
    // Arrange
    spyOn(console, 'error');

    // Act
    service.fatal('[TEST] Fatal message');

    // Assert
    expect(console.error).toHaveBeenCalled();
  });

  // ========== Message Formatting Tests ==========

  it('should_includeTimestamp_when_logMessageFormatted', () => {
    // Arrange
    spyOn(console, 'info');

    // Act
    service.info('[TEST] Message');

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toMatch(/^\[\d{4}-\d{2}-\d{2}T/);
  });

  it('should_includeLevelName_when_logMessageFormatted', () => {
    // Arrange
    spyOn(console, 'info');

    // Act
    service.info('[TEST] Message');

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toContain('[INFO]');
  });

  it('should_includeContext_when_contextProvided', () => {
    // Arrange
    spyOn(console, 'info');
    const context = { userId: 123, action: 'login' };

    // Act
    service.info('[AUTH] User logged in', context);

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toContain('"userId":123');
    expect(message).toContain('"action":"login"');
  });

  it('should_notIncludeContext_when_contextNotProvided', () => {
    // Arrange
    spyOn(console, 'info');

    // Act
    service.info('[TEST] Simple message');

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toContain('[TEST] Simple message');
  });

  // ========== Complex Context Tests ==========

  it('should_handleNestedContext_when_contextHasNestedObjects', () => {
    // Arrange
    spyOn(console, 'info');
    const context = {
      user: { id: 1, name: 'Test' },
      metadata: { timestamp: 123456 },
    };

    // Act
    service.info('[TEST] Complex context', context);

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toContain('"user"');
    expect(message).toContain('"metadata"');
  });

  it('should_handleArrayContext_when_contextIsArray', () => {
    // Arrange
    spyOn(console, 'info');
    const context = [1, 2, 3];

    // Act
    service.info('[TEST] Array context', context);

    // Assert
    const call = (console.info as jasmine.Spy).calls.mostRecent();
    const message = call.args[0] as string;
    expect(message).toContain('[1,2,3]');
  });

  // ========== Debug Method Tests ==========

  it('should_haveDebugMethod_when_serviceCalled', () => {
    // Arrange / Act / Assert
    expect(service.debug).toBeDefined();
    expect(typeof service.debug).toBe('function');
  });

  it('should_haveTraceMethod_when_serviceCalled', () => {
    // Arrange / Act / Assert
    expect(service.trace).toBeDefined();
    expect(typeof service.trace).toBe('function');
  });

  // ========== All Log Methods Exist Tests ==========

  it('should_haveAllLogMethods_when_serviceCalled', () => {
    // Assert
    expect(service.trace).toBeDefined();
    expect(service.debug).toBeDefined();
    expect(service.info).toBeDefined();
    expect(service.warn).toBeDefined();
    expect(service.error).toBeDefined();
    expect(service.fatal).toBeDefined();
  });
});
