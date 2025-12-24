import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideToastr, ToastrService } from 'ngx-toastr';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminAuditListComponent } from './audit-list.component';
import { AuditService } from '../../../../services/audit.service';
import { LoggerService } from '../../../../services/logger.service';
import { AuditLogResponse, Page } from '../../../../models/audit.model';

describe('AdminAuditListComponent', () => {
  let component: AdminAuditListComponent;
  let fixture: ComponentFixture<AdminAuditListComponent>;
  let auditService: jasmine.SpyObj<AuditService>;
  let loggerService: jasmine.SpyObj<LoggerService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let _translateService: jasmine.SpyObj<TranslateService>;

  const mockAuditLogs: AuditLogResponse[] = [
    {
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
    },
    {
      id: 2,
      action: 'DELETE',
      entityType: 'Skill',
      entityId: 5,
      entityName: 'Test Skill',
      username: 'admin',
      userRole: 'ADMIN',
      ipAddress: '127.0.0.1',
      success: false,
      errorMessage: 'Constraint violation',
      createdAt: '2024-01-15T11:00:00',
    },
  ];

  const mockPageResponse: Page<AuditLogResponse> = {
    content: mockAuditLogs,
    totalPages: 5,
    totalElements: 100,
    size: 20,
    number: 0,
    first: true,
    last: false,
  };

  const mockActions = ['CREATE', 'UPDATE', 'DELETE', 'PUBLISH'];
  const mockEntityTypes = ['Project', 'Skill', 'Experience', 'Article'];

  beforeEach(async () => {
    const auditServiceSpy = jasmine.createSpyObj('AuditService', [
      'getAuditLogs',
      'getActions',
      'getEntityTypes',
      'exportCsv',
      'exportJson',
    ]);
    const loggerServiceSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'debug']);
    const toastrServiceSpy = jasmine.createSpyObj('ToastrService', ['info', 'error']);
    const translateServiceSpy = jasmine.createSpyObj('TranslateService', ['instant']);
    translateServiceSpy.instant.and.callFake((key: string) => key);

    await TestBed.configureTestingModule({
      imports: [AdminAuditListComponent, TranslateModule.forRoot()],
      providers: [
        provideToastr(),
        { provide: AuditService, useValue: auditServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy },
        { provide: ToastrService, useValue: toastrServiceSpy },
        { provide: TranslateService, useValue: translateServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAuditListComponent);
    component = fixture.componentInstance;
    auditService = TestBed.inject(AuditService) as jasmine.SpyObj<AuditService>;
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastrService = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;
    _translateService = TestBed.inject(TranslateService) as jasmine.SpyObj<TranslateService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load filter options and audit logs on init', () => {
      auditService.getActions.and.returnValue(of(mockActions));
      auditService.getEntityTypes.and.returnValue(of(mockEntityTypes));
      auditService.getAuditLogs.and.returnValue(of(mockPageResponse));

      component.ngOnInit();

      expect(auditService.getActions).toHaveBeenCalled();
      expect(auditService.getEntityTypes).toHaveBeenCalled();
      expect(auditService.getAuditLogs).toHaveBeenCalledWith({}, 0, 20);
      expect(component.actions).toEqual(mockActions);
      expect(component.entityTypes).toEqual(mockEntityTypes);
      expect(component.auditLogs).toEqual(mockAuditLogs);
      expect(component.totalElements).toBe(100);
      expect(component.totalPages).toBe(5);
      expect(component.loading).toBe(false);
    });

    it('should handle error when loading audit logs fails', () => {
      auditService.getActions.and.returnValue(of(mockActions));
      auditService.getEntityTypes.and.returnValue(of(mockEntityTypes));
      auditService.getAuditLogs.and.returnValue(throwError(() => new Error('Server error')));

      component.ngOnInit();

      expect(component.error).toBe('admin.audit.loadError');
      expect(component.loading).toBe(false);
      expect(toastrService.error).toHaveBeenCalledWith('admin.audit.loadError');
      expect(loggerService.error).toHaveBeenCalledWith(
        '[HTTP_ERROR] Failed to load audit logs',
        jasmine.objectContaining({ error: jasmine.anything() })
      );
    });

    it('should handle error when loading actions fails', () => {
      auditService.getActions.and.returnValue(throwError(() => new Error('Server error')));
      auditService.getEntityTypes.and.returnValue(of(mockEntityTypes));
      auditService.getAuditLogs.and.returnValue(of(mockPageResponse));

      component.ngOnInit();

      expect(loggerService.error).toHaveBeenCalledWith(
        '[HTTP_ERROR] Failed to load actions',
        jasmine.objectContaining({ error: jasmine.anything() })
      );
    });
  });

  describe('applyFilters', () => {
    it('should reset to first page and reload logs', () => {
      auditService.getAuditLogs.and.returnValue(of(mockPageResponse));
      component.currentPage = 3;
      component.filter = { action: 'CREATE' };

      component.applyFilters();

      expect(component.currentPage).toBe(0);
      expect(auditService.getAuditLogs).toHaveBeenCalledWith({ action: 'CREATE' }, 0, 20);
    });
  });

  describe('clearFilters', () => {
    it('should clear all filters and reload logs', () => {
      auditService.getAuditLogs.and.returnValue(of(mockPageResponse));
      component.filter = { action: 'CREATE', entityType: 'Project' };
      component.currentPage = 2;

      component.clearFilters();

      expect(component.filter).toEqual({});
      expect(component.currentPage).toBe(0);
      expect(auditService.getAuditLogs).toHaveBeenCalledWith({}, 0, 20);
    });
  });

  describe('goToPage', () => {
    beforeEach(() => {
      auditService.getAuditLogs.and.returnValue(of(mockPageResponse));
      component.totalPages = 5;
    });

    it('should navigate to valid page', () => {
      component.goToPage(2);

      expect(component.currentPage).toBe(2);
      expect(auditService.getAuditLogs).toHaveBeenCalledWith({}, 2, 20);
    });

    it('should not navigate to negative page', () => {
      component.currentPage = 0;

      component.goToPage(-1);

      expect(component.currentPage).toBe(0);
    });

    it('should not navigate beyond total pages', () => {
      component.currentPage = 4;

      component.goToPage(5);

      expect(component.currentPage).toBe(4);
    });
  });

  describe('exportCsv', () => {
    it('should call export and show info toast', () => {
      component.filter = { action: 'CREATE' };

      component.exportCsv();

      expect(auditService.exportCsv).toHaveBeenCalledWith({ action: 'CREATE' });
      expect(toastrService.info).toHaveBeenCalledWith('admin.audit.exporting');
    });
  });

  describe('exportJson', () => {
    it('should call export and show info toast', () => {
      component.filter = { entityType: 'Project' };

      component.exportJson();

      expect(auditService.exportJson).toHaveBeenCalledWith({ entityType: 'Project' });
      expect(toastrService.info).toHaveBeenCalledWith('admin.audit.exporting');
    });
  });

  describe('getActionBadgeClass', () => {
    it('should return bg-success for CREATE action', () => {
      expect(component.getActionBadgeClass('CREATE')).toBe('bg-success');
    });

    it('should return bg-primary for UPDATE action', () => {
      expect(component.getActionBadgeClass('UPDATE')).toBe('bg-primary');
    });

    it('should return bg-danger for DELETE action', () => {
      expect(component.getActionBadgeClass('DELETE')).toBe('bg-danger');
    });

    it('should return bg-info for PUBLISH action', () => {
      expect(component.getActionBadgeClass('PUBLISH')).toBe('bg-info');
    });

    it('should return bg-warning for UNPUBLISH action', () => {
      expect(component.getActionBadgeClass('UNPUBLISH')).toBe('bg-warning text-dark');
    });

    it('should return bg-secondary for SET_CURRENT action', () => {
      expect(component.getActionBadgeClass('SET_CURRENT')).toBe('bg-secondary');
    });

    it('should return bg-success for LOGIN action', () => {
      expect(component.getActionBadgeClass('LOGIN')).toBe('bg-success');
    });

    it('should return bg-secondary for LOGOUT action', () => {
      expect(component.getActionBadgeClass('LOGOUT')).toBe('bg-secondary');
    });

    it('should return bg-warning for PASSWORD_CHANGE action', () => {
      expect(component.getActionBadgeClass('PASSWORD_CHANGE')).toBe('bg-warning text-dark');
    });

    it('should return bg-secondary for unknown action', () => {
      expect(component.getActionBadgeClass('UNKNOWN')).toBe('bg-secondary');
    });
  });

  describe('getStatusBadgeClass', () => {
    it('should return bg-success for successful status', () => {
      expect(component.getStatusBadgeClass(true)).toBe('bg-success');
    });

    it('should return bg-danger for failed status', () => {
      expect(component.getStatusBadgeClass(false)).toBe('bg-danger');
    });
  });

  describe('formatDate', () => {
    it('should format date in French locale', () => {
      const result = component.formatDate('2024-01-15T10:30:00');

      expect(result).toContain('2024');
      expect(result).toContain('10');
      expect(result).toContain('30');
    });

    it('should return dash for empty date', () => {
      expect(component.formatDate('')).toBe('-');
    });
  });

  describe('pageNumbers', () => {
    it('should return correct page numbers for first page', () => {
      component.currentPage = 0;
      component.totalPages = 10;

      const pages = component.pageNumbers;

      expect(pages).toEqual([0, 1, 2, 3, 4]);
    });

    it('should return correct page numbers for middle page', () => {
      component.currentPage = 5;
      component.totalPages = 10;

      const pages = component.pageNumbers;

      expect(pages).toEqual([3, 4, 5, 6, 7]);
    });

    it('should return correct page numbers for last page', () => {
      component.currentPage = 9;
      component.totalPages = 10;

      const pages = component.pageNumbers;

      expect(pages).toEqual([5, 6, 7, 8, 9]);
    });

    it('should return all pages when total is less than max visible', () => {
      component.currentPage = 1;
      component.totalPages = 3;

      const pages = component.pageNumbers;

      expect(pages).toEqual([0, 1, 2]);
    });
  });
});
