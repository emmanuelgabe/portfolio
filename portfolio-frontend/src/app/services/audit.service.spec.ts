import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuditService } from './audit.service';
import { LoggerService } from './logger.service';
import { AuditLogResponse, AuditLogFilter, Page } from '../models/audit.model';
import { environment } from '../../environments/environment';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  let loggerService: jasmine.SpyObj<LoggerService>;
  const apiUrl = `${environment.apiUrl}/api/admin/audit`;

  const mockAuditLogResponse: AuditLogResponse = {
    id: 1,
    action: 'CREATE',
    entityType: 'Project',
    entityId: 10,
    entityName: 'Test Project',
    username: 'admin',
    userRole: 'ADMIN',
    ipAddress: '127.0.0.1',
    success: true,
    createdAt: '2024-01-15T10:30:00',
  };

  const mockPageResponse: Page<AuditLogResponse> = {
    content: [mockAuditLogResponse],
    totalPages: 5,
    totalElements: 100,
    size: 20,
    number: 0,
    first: true,
    last: false,
  };

  beforeEach(() => {
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'debug']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        AuditService,
        { provide: LoggerService, useValue: loggerSpy },
      ],
    });

    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAuditLogs', () => {
    it('should fetch paginated audit logs with default parameters', () => {
      service.getAuditLogs().subscribe((page) => {
        expect(page).toEqual(mockPageResponse);
        expect(page.content.length).toBe(1);
      });

      const req = httpMock.expectOne((request) => request.url === apiUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      expect(req.request.params.get('sort')).toBe('createdAt,desc');
      req.flush(mockPageResponse);
    });

    it('should fetch audit logs with custom page and size', () => {
      service.getAuditLogs({}, 2, 50).subscribe((page) => {
        expect(page).toEqual(mockPageResponse);
      });

      const req = httpMock.expectOne((request) => request.url === apiUrl);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('50');
      req.flush(mockPageResponse);
    });

    it('should include filter parameters when provided', () => {
      const filter: AuditLogFilter = {
        action: 'CREATE',
        entityType: 'Project',
        entityId: 5,
        username: 'admin',
        success: true,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
      };

      service.getAuditLogs(filter, 0, 20).subscribe((page) => {
        expect(page).toEqual(mockPageResponse);
      });

      const req = httpMock.expectOne((request) => request.url === apiUrl);
      expect(req.request.params.get('action')).toBe('CREATE');
      expect(req.request.params.get('entityType')).toBe('Project');
      expect(req.request.params.get('entityId')).toBe('5');
      expect(req.request.params.get('username')).toBe('admin');
      expect(req.request.params.get('success')).toBe('true');
      expect(req.request.params.get('startDate')).toBe('2024-01-01');
      expect(req.request.params.get('endDate')).toBe('2024-12-31');
      req.flush(mockPageResponse);
    });

    it('should log error when fetch fails', () => {
      service.getAuditLogs().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch audit logs failed',
            jasmine.objectContaining({
              filter: {},
              page: 0,
              size: 20,
            })
          );
        },
      });

      const req = httpMock.expectOne((request) => request.url === apiUrl);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getEntityHistory', () => {
    it('should fetch entity history', () => {
      const entityType = 'Project';
      const entityId = 10;
      const mockHistory: AuditLogResponse[] = [mockAuditLogResponse];

      service.getEntityHistory(entityType, entityId).subscribe((history) => {
        expect(history).toEqual(mockHistory);
        expect(history.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/entity/${entityType}/${entityId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHistory);
    });

    it('should log error when fetch entity history fails', () => {
      const entityType = 'Project';
      const entityId = 999;

      service.getEntityHistory(entityType, entityId).subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch entity history failed',
            jasmine.objectContaining({
              entityType,
              entityId,
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/entity/${entityType}/${entityId}`);
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getActions', () => {
    it('should fetch available actions', () => {
      const mockActions = ['CREATE', 'UPDATE', 'DELETE', 'PUBLISH'];

      service.getActions().subscribe((actions) => {
        expect(actions).toEqual(mockActions);
        expect(actions.length).toBe(4);
      });

      const req = httpMock.expectOne(`${apiUrl}/actions`);
      expect(req.request.method).toBe('GET');
      req.flush(mockActions);
    });

    it('should log error when fetch actions fails', () => {
      service.getActions().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch audit actions failed',
            jasmine.objectContaining({
              error: jasmine.anything(),
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/actions`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getEntityTypes', () => {
    it('should fetch available entity types', () => {
      const mockTypes = ['Project', 'Skill', 'Experience', 'Article'];

      service.getEntityTypes().subscribe((types) => {
        expect(types).toEqual(mockTypes);
        expect(types.length).toBe(4);
      });

      const req = httpMock.expectOne(`${apiUrl}/entity-types`);
      expect(req.request.method).toBe('GET');
      req.flush(mockTypes);
    });

    it('should log error when fetch entity types fails', () => {
      service.getEntityTypes().subscribe({
        next: () => fail('should have failed'),
        error: () => {
          expect(loggerService.error).toHaveBeenCalledWith(
            '[HTTP_ERROR] Fetch entity types failed',
            jasmine.objectContaining({
              error: jasmine.anything(),
            })
          );
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/entity-types`);
      req.flush('Server error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('exportCsv', () => {
    it('should request CSV export and trigger download', () => {
      const mockBlob = new Blob(['test,data'], { type: 'text/csv' });
      const createObjectURLSpy = spyOn(window.URL, 'createObjectURL').and.returnValue(
        'blob:test-url'
      );
      const revokeObjectURLSpy = spyOn(window.URL, 'revokeObjectURL');

      service.exportCsv({});

      expect(loggerService.info).toHaveBeenCalledWith(
        '[HTTP_REQUEST] GET /api/admin/audit/export/csv'
      );

      const req = httpMock.expectOne(`${apiUrl}/export/csv`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);

      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:test-url');
      expect(loggerService.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Audit CSV exported');
    });

    it('should include filter parameters in CSV export', () => {
      const filter: AuditLogFilter = {
        action: 'CREATE',
        entityType: 'Project',
        startDate: '2024-01-01',
        endDate: '2024-12-31',
      };
      const mockBlob = new Blob(['test,data'], { type: 'text/csv' });
      spyOn(window.URL, 'createObjectURL').and.returnValue('blob:test-url');
      spyOn(window.URL, 'revokeObjectURL');

      service.exportCsv(filter);

      const req = httpMock.expectOne((request) => request.url === `${apiUrl}/export/csv`);
      expect(req.request.params.get('action')).toBe('CREATE');
      expect(req.request.params.get('entityType')).toBe('Project');
      expect(req.request.params.get('startDate')).toBe('2024-01-01');
      expect(req.request.params.get('endDate')).toBe('2024-12-31');
      req.flush(mockBlob);
    });

    it('should log error when CSV export fails', () => {
      service.exportCsv({});

      const req = httpMock.expectOne(`${apiUrl}/export/csv`);
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      expect(loggerService.error).toHaveBeenCalledWith(
        '[HTTP_ERROR] Export CSV failed',
        jasmine.objectContaining({
          error: jasmine.anything(),
        })
      );
    });
  });

  describe('exportJson', () => {
    it('should request JSON export and trigger download', () => {
      const mockBlob = new Blob(['{"test":"data"}'], { type: 'application/json' });
      const createObjectURLSpy = spyOn(window.URL, 'createObjectURL').and.returnValue(
        'blob:test-url'
      );
      const revokeObjectURLSpy = spyOn(window.URL, 'revokeObjectURL');

      service.exportJson({});

      expect(loggerService.info).toHaveBeenCalledWith(
        '[HTTP_REQUEST] GET /api/admin/audit/export/json'
      );

      const req = httpMock.expectOne(`${apiUrl}/export/json`);
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(mockBlob);

      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:test-url');
      expect(loggerService.info).toHaveBeenCalledWith('[HTTP_SUCCESS] Audit JSON exported');
    });

    it('should log error when JSON export fails', () => {
      service.exportJson({});

      const req = httpMock.expectOne(`${apiUrl}/export/json`);
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      expect(loggerService.error).toHaveBeenCalledWith(
        '[HTTP_ERROR] Export JSON failed',
        jasmine.objectContaining({
          error: jasmine.anything(),
        })
      );
    });
  });
});
