import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HealthMonitorService } from './health-monitor.service';
import { LoggerService } from './logger.service';
import { TokenStorageService } from './token-storage.service';
import {
  ActionDecisionRequest,
  HealthReport,
  ProposedAction,
} from '../models/health-monitor.model';
import { environment } from '../../environments/environment';

describe('HealthMonitorService', () => {
  let service: HealthMonitorService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api/admin/health-monitor`;

  const mockReport: HealthReport = {
    id: 1,
    cycleType: 'SCHEDULED',
    severity: 'OK',
    summary: 'All services healthy',
    anomalies: [],
    toolCalls: ['checkServiceHealth'],
    durationMs: 1200,
    createdAt: '2026-04-11T10:00:00Z',
  };

  const mockPendingAction: ProposedAction = {
    id: 10,
    reportId: 1,
    level: 'ORANGE',
    actionType: 'FLUSH_REDIS_CACHE',
    params: { pattern: 'session:*' },
    justification: 'Cache saturated',
    status: 'PENDING',
    approvedBy: undefined,
    approvedAt: undefined,
    executedAt: undefined,
    rejectionReason: undefined,
    resultMessage: undefined,
    expiresAt: '2026-04-11T11:00:00Z',
    createdAt: '2026-04-11T10:00:00Z',
  };

  const mockExecutedAction: ProposedAction = {
    ...mockPendingAction,
    id: 11,
    status: 'EXECUTED',
    approvedBy: 'admin',
    approvedAt: '2026-04-11T10:05:00Z',
    executedAt: '2026-04-11T10:05:30Z',
    resultMessage: 'Cache flushed',
  };

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'debug', 'warn']);
    const tokenSpy = jasmine.createSpyObj('TokenStorageService', ['getAccessToken']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        HealthMonitorService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TokenStorageService, useValue: tokenSpy },
      ],
    });

    service = TestBed.inject(HealthMonitorService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ========== HTTP: loadRecentReports ==========

  describe('loadRecentReports', () => {
    it('should_fetchReportsWithDefaultLimit_when_loadRecentReportsCalledWithoutArgs', () => {
      service.loadRecentReports().subscribe((reports) => {
        expect(reports).toEqual([mockReport]);
      });

      const req = httpMock.expectOne(`${apiUrl}/reports?limit=20`);
      expect(req.request.method).toBe('GET');
      req.flush([mockReport]);
    });

    it('should_fetchReportsWithCustomLimit_when_loadRecentReportsCalledWithLimit', () => {
      service.loadRecentReports(5).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/reports?limit=5`);
      expect(req.request.method).toBe('GET');
      req.flush([mockReport]);
    });

    it('should_logError_when_loadRecentReportsFails', () => {
      service.loadRecentReports().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] Fetch reports failed',
            jasmine.objectContaining({ status: 500 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/reports?limit=20`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== HTTP: loadPendingActions ==========

  describe('loadPendingActions', () => {
    it('should_fetchPendingActions_when_loadPendingActionsCalled', () => {
      service.loadPendingActions().subscribe((actions) => {
        expect(actions).toEqual([mockPendingAction]);
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/pending`);
      expect(req.request.method).toBe('GET');
      req.flush([mockPendingAction]);
    });

    it('should_logError_when_loadPendingActionsFails', () => {
      service.loadPendingActions().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] Fetch pending actions failed',
            jasmine.objectContaining({ status: 403 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/pending`);
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });
  });

  // ========== HTTP: loadActionHistory ==========

  describe('loadActionHistory', () => {
    it('should_fetchHistoryWithDefaultLimit_when_loadActionHistoryCalledWithoutArgs', () => {
      service.loadActionHistory().subscribe((actions) => {
        expect(actions.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}/actions?limit=50`);
      expect(req.request.method).toBe('GET');
      req.flush([mockPendingAction, mockExecutedAction]);
    });

    it('should_fetchHistoryWithCustomLimit_when_loadActionHistoryCalledWithLimit', () => {
      service.loadActionHistory(100).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/actions?limit=100`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('should_logError_when_loadActionHistoryFails', () => {
      service.loadActionHistory().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] Fetch action history failed',
            jasmine.objectContaining({ status: 500 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/actions?limit=50`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== HTTP: triggerAnalysis ==========

  describe('triggerAnalysis', () => {
    it('should_returnReport_when_backendRespondsWith200AndBody', () => {
      service.triggerAnalysis().subscribe((report) => {
        expect(report).toEqual(mockReport);
      });

      const req = httpMock.expectOne(`${apiUrl}/analyze-now`);
      expect(req.request.method).toBe('POST');
      req.flush(mockReport, { status: 200, statusText: 'OK' });
    });

    it('should_returnUndefined_when_backendRespondsWith204', () => {
      service.triggerAnalysis().subscribe((report) => {
        expect(report).toBeUndefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/analyze-now`);
      req.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('should_logError_when_triggerAnalysisFails', () => {
      service.triggerAnalysis().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] analyze-now failed',
            jasmine.objectContaining({ status: 500 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/analyze-now`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== HTTP: approveAction ==========

  describe('approveAction', () => {
    it('should_postApproveRequestWithReason_when_approveActionCalled', () => {
      const request: ActionDecisionRequest = { reason: 'Looks good' };

      service.approveAction(10, request).subscribe((updated) => {
        expect(updated).toEqual(mockExecutedAction);
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/10/approve`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockExecutedAction);
    });

    it('should_logError_when_approveActionFails', () => {
      const request: ActionDecisionRequest = { reason: undefined };

      service.approveAction(10, request).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] Approve failed',
            jasmine.objectContaining({ id: 10, status: 400 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/10/approve`);
      req.flush('Bad request', { status: 400, statusText: 'Bad Request' });
    });
  });

  // ========== HTTP: rejectAction ==========

  describe('rejectAction', () => {
    it('should_postRejectRequestWithReason_when_rejectActionCalled', () => {
      const request: ActionDecisionRequest = { reason: 'Too risky' };
      const rejected: ProposedAction = { ...mockPendingAction, status: 'REJECTED' };

      service.rejectAction(10, request).subscribe((updated) => {
        expect(updated.status).toBe('REJECTED');
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/10/reject`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(rejected);
    });

    it('should_logError_when_rejectActionFails', () => {
      const request: ActionDecisionRequest = { reason: 'Too risky' };

      service.rejectAction(10, request).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HEALTH_MONITOR] Reject failed',
            jasmine.objectContaining({ id: 10, status: 500 })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/actions/10/reject`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ========== State management ==========

  describe('state management', () => {
    it('should_emitUpdatedList_when_setReportsCalled', (done) => {
      service.setReports([mockReport]);
      service.reports$.subscribe((reports) => {
        expect(reports).toEqual([mockReport]);
        done();
      });
    });

    it('should_emitUpdatedList_when_setPendingActionsCalled', (done) => {
      service.setPendingActions([mockPendingAction]);
      service.pendingActions$.subscribe((actions) => {
        expect(actions).toEqual([mockPendingAction]);
        done();
      });
    });

    it('should_insertReportAtHead_when_prependReportCalled', () => {
      const older: HealthReport = { ...mockReport, id: 99 };
      service.setReports([older]);
      service.prependReport(mockReport);

      expect(service.getReportsSnapshot()).toEqual([mockReport, older]);
    });

    it('should_insertActionAtHead_when_prependPendingActionCalled', () => {
      const older: ProposedAction = { ...mockPendingAction, id: 99 };
      service.setPendingActions([older]);
      service.prependPendingAction(mockPendingAction);

      expect(service.getPendingActionsSnapshot()).toEqual([mockPendingAction, older]);
    });

    it('should_replacePendingAction_when_upsertActionCalledWithPendingStatus', () => {
      const existing: ProposedAction = { ...mockPendingAction, id: 10 };
      const other: ProposedAction = { ...mockPendingAction, id: 20 };
      service.setPendingActions([existing, other]);

      const updated: ProposedAction = {
        ...existing,
        justification: 'Updated justification',
      };
      service.upsertAction(updated);

      const snapshot = service.getPendingActionsSnapshot();
      expect(snapshot.length).toBe(2);
      expect(snapshot[0].id).toBe(10);
      expect(snapshot[0].justification).toBe('Updated justification');
    });

    it('should_removeFromPending_when_upsertActionCalledWithNonPendingStatus', () => {
      const first: ProposedAction = { ...mockPendingAction, id: 10 };
      const second: ProposedAction = { ...mockPendingAction, id: 20 };
      service.setPendingActions([first, second]);

      const executed: ProposedAction = { ...first, status: 'EXECUTED' };
      service.upsertAction(executed);

      const snapshot = service.getPendingActionsSnapshot();
      expect(snapshot.length).toBe(1);
      expect(snapshot[0].id).toBe(20);
    });

    it('should_returnCurrentReports_when_getReportsSnapshotCalled', () => {
      service.setReports([mockReport]);
      expect(service.getReportsSnapshot()).toEqual([mockReport]);
    });

    it('should_returnCurrentPendingActions_when_getPendingActionsSnapshotCalled', () => {
      service.setPendingActions([mockPendingAction]);
      expect(service.getPendingActionsSnapshot()).toEqual([mockPendingAction]);
    });
  });
});
