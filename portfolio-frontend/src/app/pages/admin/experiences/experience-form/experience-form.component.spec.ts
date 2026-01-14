import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ToastrModule } from 'ngx-toastr';
import { of, throwError } from 'rxjs';
import { ExperienceFormComponent } from './experience-form.component';
import { ExperienceService } from '../../../../services/experience.service';
import { LoggerService } from '../../../../services/logger.service';
import { DemoModeService } from '../../../../services/demo-mode.service';
import { ExperienceResponse, ExperienceType } from '../../../../models';

describe('ExperienceFormComponent', () => {
  let component: ExperienceFormComponent;
  let fixture: ComponentFixture<ExperienceFormComponent>;
  let experienceService: jasmine.SpyObj<ExperienceService>;
  let loggerService: jasmine.SpyObj<LoggerService>;
  let router: jasmine.SpyObj<Router>;
  let activatedRoute: ActivatedRoute;

  const mockExperienceResponse: ExperienceResponse = {
    id: 1,
    company: 'Test Company',
    role: 'Software Engineer',
    startDate: '2022-01-01',
    endDate: '2023-12-31',
    description: 'Test description',
    type: ExperienceType.WORK,
    createdAt: '2024-01-01T00:00:00',
    updatedAt: '2024-01-01T00:00:00',
    ongoing: false,
  };

  beforeEach(async () => {
    const experienceServiceSpy = jasmine.createSpyObj('ExperienceService', [
      'getById',
      'create',
      'update',
    ]);
    const loggerServiceSpy = jasmine.createSpyObj('LoggerService', ['error', 'info']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const demoModeServiceSpy = jasmine.createSpyObj('DemoModeService', ['isDemo']);
    demoModeServiceSpy.isDemo.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [
        ExperienceFormComponent,
        ReactiveFormsModule,
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        ToastrModule.forRoot(),
      ],
      providers: [
        { provide: ExperienceService, useValue: experienceServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: DemoModeService, useValue: demoModeServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: jasmine.createSpy('get').and.returnValue(null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExperienceFormComponent);
    component = fixture.componentInstance;
    experienceService = TestBed.inject(ExperienceService) as jasmine.SpyObj<ExperienceService>;
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    activatedRoute = TestBed.inject(ActivatedRoute);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with empty values for create mode', () => {
      expect(component.experienceForm).toBeDefined();
      expect(component.experienceForm.get('company')?.value).toBe('');
      expect(component.experienceForm.get('role')?.value).toBe('');
      expect(component.experienceForm.get('startDate')?.value).toBe('');
      expect(component.experienceForm.get('endDate')?.value).toBe('');
      expect(component.experienceForm.get('description')?.value).toBe('');
      expect(component.experienceForm.get('type')?.value).toBe(ExperienceType.WORK);
    });

    it('should validate required fields', () => {
      const form = component.experienceForm;

      expect(form.get('company')?.hasError('required')).toBe(true);
      expect(form.get('role')?.hasError('required')).toBe(true);
      expect(form.get('startDate')?.hasError('required')).toBe(true);
      expect(form.get('description')?.hasError('required')).toBe(true);
      // type has default value ExperienceType.WORK, so no required error
      expect(form.get('type')?.hasError('required')).toBe(false);
    });

    it('should validate minimum length for company', () => {
      const companyControl = component.experienceForm.get('company');
      companyControl?.setValue('A');

      expect(companyControl?.hasError('minlength')).toBe(true);

      companyControl?.setValue('AB');
      expect(companyControl?.hasError('minlength')).toBe(false);
    });

    it('should validate minimum length for description', () => {
      const descControl = component.experienceForm.get('description');
      descControl?.setValue('Short');

      expect(descControl?.hasError('minlength')).toBe(true);

      descControl?.setValue('This is a valid description that is longer than 10 characters');
      expect(descControl?.hasError('minlength')).toBe(false);
    });
  });

  describe('Edit Mode', () => {
    beforeEach(() => {
      activatedRoute.snapshot.paramMap.get = jasmine.createSpy('get').and.returnValue('1');
    });

    it('should load experience in edit mode', () => {
      experienceService.getById.and.returnValue(of(mockExperienceResponse));

      component.checkMode();

      expect(component.isEditMode).toBe(true);
      expect(component.experienceId).toBe(1);
      expect(experienceService.getById).toHaveBeenCalledWith(1);
    });

    it('should patch form values when loading experience', () => {
      experienceService.getById.and.returnValue(of(mockExperienceResponse));

      component.loadExperience(1);

      expect(component.experienceForm.get('company')?.value).toBe(mockExperienceResponse.company);
      expect(component.experienceForm.get('role')?.value).toBe(mockExperienceResponse.role);
      expect(component.experienceForm.get('description')?.value).toBe(
        mockExperienceResponse.description
      );
      expect(component.loading).toBe(false);
    });

    it('should handle error when loading experience fails', () => {
      const errorResponse = { message: 'Not found' };
      experienceService.getById.and.returnValue(throwError(() => errorResponse));

      component.loadExperience(1);

      expect(loggerService.error).toHaveBeenCalledWith('[HTTP_ERROR] Failed to load experience', {
        id: 1,
        error: errorResponse.message,
      });
      expect(router.navigate).toHaveBeenCalledWith(['/admin/experiences']);
    });
  });

  describe('onSubmit', () => {
    it('should not submit if form is invalid', () => {
      component.experienceForm.patchValue({
        company: '',
        role: '',
      });

      component.onSubmit();

      expect(experienceService.create).not.toHaveBeenCalled();
      expect(experienceService.update).not.toHaveBeenCalled();
    });

    it('should create experience when in create mode', () => {
      const formValue = {
        company: 'New Company',
        role: 'Developer',
        startDate: '2023-01-01',
        endDate: '',
        description: 'This is a valid description for testing purposes',
        type: ExperienceType.WORK,
      };

      component.experienceForm.patchValue(formValue);
      experienceService.create.and.returnValue(of(mockExperienceResponse));

      component.onSubmit();

      expect(experienceService.create).toHaveBeenCalledWith(
        jasmine.objectContaining({
          company: 'New Company',
          role: 'Developer',
          startDate: '2023-01-01',
          endDate: null,
        })
      );
      expect(router.navigate).toHaveBeenCalledWith(['/admin/experiences']);
    });

    it('should handle error when creation fails', () => {
      const formValue = {
        company: 'New Company',
        role: 'Developer',
        startDate: '2023-01-01',
        endDate: undefined,
        description: 'This is a valid description for testing purposes',
        type: ExperienceType.WORK,
      };

      const errorResponse = { message: 'Validation failed' };
      component.experienceForm.patchValue(formValue);
      experienceService.create.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(loggerService.error).toHaveBeenCalledWith('[HTTP_ERROR] Failed to create experience', {
        company: formValue.company,
        error: errorResponse.message,
      });
      expect(component.submitting).toBe(false);
    });

    it('should update experience when in edit mode', () => {
      const formValue = {
        company: 'Updated Company',
        role: 'Senior Developer',
        startDate: '2023-01-01',
        endDate: '',
        description: 'This is an updated description for testing purposes',
        type: ExperienceType.WORK,
      };

      component.isEditMode = true;
      component.experienceId = 1;
      component.experienceForm.patchValue(formValue);
      experienceService.update.and.returnValue(of(mockExperienceResponse));

      component.onSubmit();

      expect(experienceService.update).toHaveBeenCalledWith(
        1,
        jasmine.objectContaining({
          company: 'Updated Company',
          role: 'Senior Developer',
          startDate: '2023-01-01',
          endDate: null,
        })
      );
      expect(router.navigate).toHaveBeenCalledWith(['/admin/experiences']);
    });

    it('should handle error when update fails', () => {
      const formValue = {
        company: 'Updated Company',
        role: 'Senior Developer',
        startDate: '2023-01-01',
        endDate: undefined,
        description: 'This is an updated description for testing purposes',
        type: ExperienceType.WORK,
      };

      const errorResponse = { message: 'Not found' };
      component.isEditMode = true;
      component.experienceId = 1;
      component.experienceForm.patchValue(formValue);
      experienceService.update.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(loggerService.error).toHaveBeenCalledWith('[HTTP_ERROR] Failed to update experience', {
        id: 1,
        error: errorResponse.message,
      });
      expect(component.submitting).toBe(false);
    });
  });

  describe('getTypeLabel', () => {
    it('should return correct label for WORK type', () => {
      expect(component.getTypeLabel(ExperienceType.WORK)).toBe('admin.experiences.work');
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
  });
});
