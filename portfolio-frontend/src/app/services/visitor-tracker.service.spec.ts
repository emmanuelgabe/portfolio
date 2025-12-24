import { TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VisitorTrackerService } from './visitor-tracker.service';
import { LoggerService } from './logger.service';
import { environment } from '../../environments/environment';

describe('VisitorTrackerService', () => {
  let service: VisitorTrackerService;
  let httpMock: HttpTestingController;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'debug', 'warn', 'error']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VisitorTrackerService, { provide: LoggerService, useValue: loggerSpy }],
    });

    service = TestBed.inject(VisitorTrackerService);
    httpMock = TestBed.inject(HttpTestingController);

    // Clear sessionStorage before each test
    sessionStorage.clear();
  });

  afterEach(() => {
    service.stopTracking();
    // Flush any remaining requests to avoid verification errors
    httpMock.match(() => true).forEach((req) => req.flush(null));
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== startTracking Tests ==========

  it('should_createSessionId_when_startTrackingCalledFirstTime', fakeAsync(() => {
    // Arrange / Act
    service.startTracking();

    // Assert
    const sessionId = sessionStorage.getItem('visitor_session_id');
    expect(sessionId).toBeTruthy();
    expect(sessionId).toMatch(/^[0-9a-f-]{36}$/i); // UUID format

    // Handle the HTTP request
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(req.request.headers.get('X-Session-Id')).toBe(sessionId);
    req.flush(null);

    discardPeriodicTasks();
  }));

  it('should_reuseSessionId_when_startTrackingCalledWithExistingSession', fakeAsync(() => {
    // Arrange
    const existingSessionId = 'existing-session-123';
    sessionStorage.setItem('visitor_session_id', existingSessionId);

    // Act
    service.startTracking();

    // Assert
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(req.request.headers.get('X-Session-Id')).toBe(existingSessionId);
    req.flush(null);

    discardPeriodicTasks();
  }));

  it('should_sendImmediateHeartbeat_when_startTrackingCalled', fakeAsync(() => {
    // Act
    service.startTracking();

    // Assert - First heartbeat is immediate
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(req.request.method).toBe('POST');
    req.flush(null);

    discardPeriodicTasks();
  }));

  it('should_logInfo_when_startTrackingCalled', fakeAsync(() => {
    // Act
    service.startTracking();

    // Handle the HTTP request
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    req.flush(null);

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[VISITOR_TRACKER] Started tracking',
      jasmine.objectContaining({ sessionId: jasmine.any(String) })
    );

    discardPeriodicTasks();
  }));

  it('should_notStartTwice_when_startTrackingCalledWhileTracking', fakeAsync(() => {
    // Arrange
    service.startTracking();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    req.flush(null);

    loggerSpy.debug.calls.reset();

    // Act
    service.startTracking();

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith('[VISITOR_TRACKER] Already tracking');

    discardPeriodicTasks();
  }));

  // ========== Interval Tests ==========

  it('should_sendPeriodicHeartbeats_when_tracking', fakeAsync(() => {
    // Arrange
    service.startTracking();

    // First immediate heartbeat
    const req1 = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(req1.request.method).toBe('POST');
    req1.flush(null);

    // Act - Wait 30 seconds
    tick(30000);

    // Assert - Second heartbeat
    const req2 = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(req2.request.method).toBe('POST');
    req2.flush(null);

    discardPeriodicTasks();
  }));

  // ========== stopTracking Tests ==========

  it('should_logInfo_when_stopTrackingCalled', fakeAsync(() => {
    // Arrange
    service.startTracking();
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    req.flush(null);

    // Act
    service.stopTracking();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[VISITOR_TRACKER] Stopped tracking');

    discardPeriodicTasks();
  }));

  it('should_stopSendingHeartbeats_when_stopTrackingCalled', fakeAsync(() => {
    // Arrange
    service.startTracking();
    const req1 = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    req1.flush(null);

    // Act
    service.stopTracking();
    tick(30000);

    // Assert - No more requests should be made
    httpMock.expectNone(`${environment.apiUrl}/api/visitors/heartbeat`);
    expect(loggerSpy.info).toHaveBeenCalledWith('[VISITOR_TRACKER] Stopped tracking');
  }));

  // ========== Error Handling Tests ==========

  it('should_logDebug_when_heartbeatFails', fakeAsync(() => {
    // Arrange
    service.startTracking();

    // Act
    const req = httpMock.expectOne(`${environment.apiUrl}/api/visitors/heartbeat`);
    req.flush(null, { status: 500, statusText: 'Server Error' });

    // Assert
    expect(loggerSpy.debug).toHaveBeenCalledWith(
      '[VISITOR_TRACKER] Heartbeat failed',
      jasmine.objectContaining({ status: 500 })
    );

    discardPeriodicTasks();
  }));
});
