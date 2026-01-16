import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { provideToastr } from 'ngx-toastr';
import { ExperienceListComponent } from './experience-list.component';
import { ExperienceService } from '../../../../services/experience.service';
import { SearchService } from '../../../../services/search.service';
import { ModalService } from '../../../../services/modal.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ExperienceResponse, ExperienceType } from '../../../../models';

describe('ExperienceListComponent', () => {
  let component: ExperienceListComponent;
  let fixture: ComponentFixture<ExperienceListComponent>;
  let experienceService: jasmine.SpyObj<ExperienceService>;
  let modalService: jasmine.SpyObj<ModalService>;
  let loggerService: jasmine.SpyObj<LoggerService>;

  const mockExperiences: ExperienceResponse[] = [
    {
      id: 1,
      company: 'Test Company 1',
      role: 'Software Engineer',
      startDate: '2022-01-01',
      endDate: '2023-12-31',
      description: 'Test description 1',
      type: ExperienceType.WORK,
      showMonths: true,
      displayOrder: 0,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00',
      ongoing: false,
    },
    {
      id: 2,
      company: 'Test Company 2',
      role: 'Senior Developer',
      startDate: '2023-01-01',
      endDate: undefined,
      description: 'Test description 2',
      type: ExperienceType.WORK,
      showMonths: true,
      displayOrder: 1,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00',
      ongoing: true,
    },
  ];

  beforeEach(async () => {
    const experienceServiceSpy = jasmine.createSpyObj('ExperienceService', [
      'getAll',
      'delete',
      'reorder',
    ]);
    const searchServiceSpy = jasmine.createSpyObj('SearchService', ['searchExperiences']);
    searchServiceSpy.searchExperiences.and.returnValue(of([]));
    const modalServiceSpy = jasmine.createSpyObj('ModalService', ['confirm']);
    const loggerServiceSpy = jasmine.createSpyObj('LoggerService', ['error', 'info']);
    const demoModeServiceSpy = jasmine.createSpyObj('DemoModeService', ['isDemo']);
    demoModeServiceSpy.isDemo.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [ExperienceListComponent, RouterModule.forRoot([]), TranslateModule.forRoot()],
      providers: [
        provideToastr(),
        { provide: ExperienceService, useValue: experienceServiceSpy },
        { provide: SearchService, useValue: searchServiceSpy },
        { provide: ModalService, useValue: modalServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy },
        { provide: DemoModeService, useValue: demoModeServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExperienceListComponent);
    component = fixture.componentInstance;
    experienceService = TestBed.inject(ExperienceService) as jasmine.SpyObj<ExperienceService>;
    modalService = TestBed.inject(ModalService) as jasmine.SpyObj<ModalService>;
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load experiences on init', () => {
      experienceService.getAll.and.returnValue(of(mockExperiences));

      component.ngOnInit();

      expect(experienceService.getAll).toHaveBeenCalled();
      expect(component.experiences).toEqual(mockExperiences);
      expect(component.loading).toBe(false);
      expect(component.error).toBeUndefined();
    });

    it('should handle error when loading experiences fails', () => {
      const errorResponse = { message: 'Server error' };
      experienceService.getAll.and.returnValue(throwError(() => errorResponse));

      component.ngOnInit();

      expect(component.error).toBe('admin.experiences.loadError');
      expect(component.loading).toBe(false);
      expect(loggerService.error).toHaveBeenCalledWith('[HTTP_ERROR] Failed to load experiences', {
        error: errorResponse.message,
      });
    });
  });

  describe('getTypeLabel', () => {
    it('should return correct label for WORK type', () => {
      expect(component.getTypeLabel(ExperienceType.WORK)).toBe('admin.experiences.work');
    });

    it('should return correct label for STAGE type', () => {
      expect(component.getTypeLabel(ExperienceType.STAGE)).toBe('admin.experiences.stage');
    });

    it('should return correct label for EDUCATION type', () => {
      expect(component.getTypeLabel(ExperienceType.EDUCATION)).toBe('admin.experiences.education');
    });

    it('should return correct label for CERTIFICATION type', () => {
      expect(component.getTypeLabel(ExperienceType.CERTIFICATION)).toBe(
        'admin.experiences.certification'
      );
    });

    it('should return correct label for VOLUNTEERING type', () => {
      expect(component.getTypeLabel(ExperienceType.VOLUNTEERING)).toBe(
        'admin.experiences.volunteering'
      );
    });

    it('should return not provided label when type is missing', () => {
      expect(component.getTypeLabel(undefined)).toBe('admin.common.notProvided');
    });

    it('should return type itself if not found in labels', () => {
      const unknownType = 'UNKNOWN' as ExperienceType;
      expect(component.getTypeLabel(unknownType)).toBe(unknownType);
    });
  });

  describe('getTypeBadgeClass', () => {
    it('should return correct badge class for WORK type', () => {
      expect(component.getTypeBadgeClass(ExperienceType.WORK)).toBe('bg-primary');
    });

    it('should return correct badge class for STAGE type', () => {
      expect(component.getTypeBadgeClass(ExperienceType.STAGE)).toBe('bg-dark');
    });

    it('should return correct badge class for EDUCATION type', () => {
      expect(component.getTypeBadgeClass(ExperienceType.EDUCATION)).toBe('bg-success');
    });

    it('should return correct badge class for CERTIFICATION type', () => {
      expect(component.getTypeBadgeClass(ExperienceType.CERTIFICATION)).toBe('bg-warning');
    });

    it('should return correct badge class for VOLUNTEERING type', () => {
      expect(component.getTypeBadgeClass(ExperienceType.VOLUNTEERING)).toBe('bg-info');
    });

    it('should return default badge class if type is missing', () => {
      expect(component.getTypeBadgeClass(undefined)).toBe('bg-secondary');
    });

    it('should return default badge class if type not found', () => {
      const unknownType = 'UNKNOWN' as ExperienceType;
      expect(component.getTypeBadgeClass(unknownType)).toBe('bg-secondary');
    });
  });

  describe('formatDate', () => {
    it('should format date in French locale', () => {
      const date = '2023-01-15';
      const result = component.formatDate(date);

      expect(result).toContain('2023');
    });
  });

  describe('confirmDelete', () => {
    it('should open confirmation modal and delete if confirmed', () => {
      const experience = mockExperiences[0];
      modalService.confirm.and.returnValue(of(true));
      experienceService.delete.and.returnValue(of(undefined));
      component.experiences = [...mockExperiences];

      component.confirmDelete(experience);

      expect(modalService.confirm).toHaveBeenCalledWith({
        title: 'admin.common.confirmDelete',
        message: 'admin.experiences.deleteConfirm',
        confirmText: 'admin.common.delete',
        cancelText: 'admin.common.cancel',
        confirmButtonClass: 'btn-danger',
        disableConfirm: false,
      });

      expect(experienceService.delete).toHaveBeenCalledWith(experience.id);
      expect(component.experiences.length).toBe(1);
      expect(component.experiences[0].id).toBe(2);
    });

    it('should not delete if user cancels confirmation', () => {
      const experience = mockExperiences[0];
      modalService.confirm.and.returnValue(of(false));
      component.experiences = [...mockExperiences];

      component.confirmDelete(experience);

      expect(modalService.confirm).toHaveBeenCalled();
      expect(experienceService.delete).not.toHaveBeenCalled();
      expect(component.experiences.length).toBe(2);
    });

    it('should handle error when deletion fails', () => {
      const experience = mockExperiences[0];
      const errorResponse = { message: 'Server error' };
      modalService.confirm.and.returnValue(of(true));
      experienceService.delete.and.returnValue(throwError(() => errorResponse));
      component.experiences = [...mockExperiences];

      component.confirmDelete(experience);

      expect(loggerService.error).toHaveBeenCalledWith('[HTTP_ERROR] Failed to delete experience', {
        id: experience.id,
        error: errorResponse.message,
      });
      expect(component.experiences.length).toBe(2);
    });
  });
});
