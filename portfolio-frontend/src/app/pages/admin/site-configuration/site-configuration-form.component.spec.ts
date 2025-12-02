import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Location } from '@angular/common';
import { of, throwError } from 'rxjs';
import { SiteConfigurationFormComponent } from './site-configuration-form.component';
import { SiteConfigurationService } from '../../../services/site-configuration.service';
import { LoggerService } from '../../../services/logger.service';
import { DemoModeService } from '../../../services/demo-mode.service';
import { ToastrService } from 'ngx-toastr';
import { SiteConfigurationResponse } from '../../../models/site-configuration.model';

describe('SiteConfigurationFormComponent', () => {
  let component: SiteConfigurationFormComponent;
  let fixture: ComponentFixture<SiteConfigurationFormComponent>;
  let siteConfigService: jasmine.SpyObj<SiteConfigurationService>;
  let loggerService: jasmine.SpyObj<LoggerService>;
  let toastrService: jasmine.SpyObj<ToastrService>;
  let location: jasmine.SpyObj<Location>;

  const mockConfig: SiteConfigurationResponse = {
    id: 1,
    fullName: 'Emmanuel Gabe',
    email: 'contact@emmanuelgabe.com',
    heroTitle: 'Developpeur Backend',
    heroDescription: 'Je cree des applications web modernes et evolutives.',
    siteTitle: 'Portfolio - Emmanuel Gabe',
    seoDescription: 'Portfolio de Emmanuel Gabe, developpeur backend Java/Spring Boot.',
    profileImageUrl: '/uploads/profile/profile.webp',
    githubUrl: 'https://github.com/emmanuelgabe',
    linkedinUrl: 'https://linkedin.com/in/egabe',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
  };

  beforeEach(async () => {
    const siteConfigServiceSpy = jasmine.createSpyObj('SiteConfigurationService', [
      'getSiteConfiguration',
      'updateSiteConfiguration',
      'uploadProfileImage',
      'deleteProfileImage',
    ]);
    const loggerServiceSpy = jasmine.createSpyObj('LoggerService', ['error', 'info']);
    const toastrServiceSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);
    const locationSpy = jasmine.createSpyObj('Location', ['back']);
    const demoModeServiceSpy = jasmine.createSpyObj('DemoModeService', ['isDemo']);
    demoModeServiceSpy.isDemo.and.returnValue(false);

    siteConfigServiceSpy.getSiteConfiguration.and.returnValue(of(mockConfig));

    await TestBed.configureTestingModule({
      imports: [SiteConfigurationFormComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [
        { provide: SiteConfigurationService, useValue: siteConfigServiceSpy },
        { provide: LoggerService, useValue: loggerServiceSpy },
        { provide: ToastrService, useValue: toastrServiceSpy },
        { provide: Location, useValue: locationSpy },
        { provide: DemoModeService, useValue: demoModeServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SiteConfigurationFormComponent);
    component = fixture.componentInstance;
    siteConfigService = TestBed.inject(
      SiteConfigurationService
    ) as jasmine.SpyObj<SiteConfigurationService>;
    loggerService = TestBed.inject(LoggerService) as jasmine.SpyObj<LoggerService>;
    toastrService = TestBed.inject(ToastrService) as jasmine.SpyObj<ToastrService>;
    location = TestBed.inject(Location) as jasmine.SpyObj<Location>;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize form with config values after loading', () => {
      expect(component.configForm).toBeDefined();
      expect(component.configForm.get('fullName')?.value).toBe(mockConfig.fullName);
      expect(component.configForm.get('email')?.value).toBe(mockConfig.email);
      expect(component.configForm.get('heroTitle')?.value).toBe(mockConfig.heroTitle);
      expect(component.configForm.get('heroDescription')?.value).toBe(mockConfig.heroDescription);
      expect(component.configForm.get('siteTitle')?.value).toBe(mockConfig.siteTitle);
      expect(component.configForm.get('seoDescription')?.value).toBe(mockConfig.seoDescription);
      expect(component.configForm.get('githubUrl')?.value).toBe(mockConfig.githubUrl);
      expect(component.configForm.get('linkedinUrl')?.value).toBe(mockConfig.linkedinUrl);
    });

    it('should validate required fields', () => {
      component.configForm.reset();
      const form = component.configForm;

      expect(form.get('fullName')?.hasError('required')).toBe(true);
      expect(form.get('email')?.hasError('required')).toBe(true);
      expect(form.get('heroTitle')?.hasError('required')).toBe(true);
      expect(form.get('heroDescription')?.hasError('required')).toBe(true);
      expect(form.get('siteTitle')?.hasError('required')).toBe(true);
      expect(form.get('seoDescription')?.hasError('required')).toBe(true);
      expect(form.get('githubUrl')?.hasError('required')).toBe(true);
      expect(form.get('linkedinUrl')?.hasError('required')).toBe(true);
    });

    it('should validate email format', () => {
      const emailControl = component.configForm.get('email');
      emailControl?.setValue('invalid-email');

      expect(emailControl?.hasError('email')).toBe(true);

      emailControl?.setValue('valid@email.com');
      expect(emailControl?.hasError('email')).toBe(false);
    });

    it('should validate URL pattern for github', () => {
      const githubControl = component.configForm.get('githubUrl');
      githubControl?.setValue('invalid-url');

      expect(githubControl?.hasError('pattern')).toBe(true);

      githubControl?.setValue('https://github.com/test');
      expect(githubControl?.hasError('pattern')).toBe(false);
    });

    it('should validate URL pattern for linkedin', () => {
      const linkedinControl = component.configForm.get('linkedinUrl');
      linkedinControl?.setValue('invalid-url');

      expect(linkedinControl?.hasError('pattern')).toBe(true);

      linkedinControl?.setValue('https://linkedin.com/in/test');
      expect(linkedinControl?.hasError('pattern')).toBe(false);
    });
  });

  describe('loadConfiguration', () => {
    it('should load configuration on init', () => {
      expect(siteConfigService.getSiteConfiguration).toHaveBeenCalled();
      expect(component.loading).toBe(false);
    });

    it('should handle error when loading configuration fails', () => {
      const errorResponse = { message: 'Failed to load' };
      siteConfigService.getSiteConfiguration.and.returnValue(throwError(() => errorResponse));

      component.loadConfiguration();

      expect(loggerService.error).toHaveBeenCalled();
      expect(toastrService.error).toHaveBeenCalled();
      expect(component.loading).toBe(false);
    });
  });

  describe('onSubmit', () => {
    it('should not submit if form is invalid', () => {
      component.configForm.patchValue({
        fullName: '',
        email: '',
      });

      component.onSubmit();

      expect(siteConfigService.updateSiteConfiguration).not.toHaveBeenCalled();
      expect(toastrService.warning).toHaveBeenCalled();
    });

    it('should update configuration when form is valid', () => {
      siteConfigService.updateSiteConfiguration.and.returnValue(of(mockConfig));

      component.onSubmit();

      expect(siteConfigService.updateSiteConfiguration).toHaveBeenCalled();
      expect(toastrService.success).toHaveBeenCalled();
      expect(component.submitting).toBe(false);
    });

    it('should handle error when update fails', () => {
      const errorResponse = { message: 'Update failed' };
      siteConfigService.updateSiteConfiguration.and.returnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(loggerService.error).toHaveBeenCalled();
      expect(toastrService.error).toHaveBeenCalled();
      expect(component.submitting).toBe(false);
    });
  });

  describe('Profile Image', () => {
    it('should upload profile image', () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.selectedFile = file;
      siteConfigService.uploadProfileImage.and.returnValue(of(mockConfig));

      component.uploadProfileImage();

      expect(siteConfigService.uploadProfileImage).toHaveBeenCalledWith(file);
      expect(toastrService.success).toHaveBeenCalled();
      expect(component.selectedFile).toBeUndefined();
      expect(component.imagePreview).toBeUndefined();
    });

    it('should handle error when upload fails', () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.selectedFile = file;
      const errorResponse = { message: 'Upload failed' };
      siteConfigService.uploadProfileImage.and.returnValue(throwError(() => errorResponse));

      component.uploadProfileImage();

      expect(loggerService.error).toHaveBeenCalled();
      expect(toastrService.error).toHaveBeenCalled();
      expect(component.uploadingImage).toBe(false);
    });

    it('should delete profile image', () => {
      component.profileImageUrl = '/uploads/profile/test.webp';
      siteConfigService.deleteProfileImage.and.returnValue(of(mockConfig));

      component.deleteProfileImage();

      expect(siteConfigService.deleteProfileImage).toHaveBeenCalled();
      expect(toastrService.success).toHaveBeenCalled();
      expect(component.profileImageUrl).toBeUndefined();
    });

    it('should handle error when delete fails', () => {
      component.profileImageUrl = '/uploads/profile/test.webp';
      const errorResponse = { message: 'Delete failed' };
      siteConfigService.deleteProfileImage.and.returnValue(throwError(() => errorResponse));

      component.deleteProfileImage();

      expect(loggerService.error).toHaveBeenCalled();
      expect(toastrService.error).toHaveBeenCalled();
      expect(component.uploadingImage).toBe(false);
    });

    it('should cancel image selection', () => {
      component.selectedFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.imagePreview = 'data:image/jpeg;base64,test';

      component.cancelImageSelection();

      expect(component.selectedFile).toBeUndefined();
      expect(component.imagePreview).toBeUndefined();
    });
  });

  describe('Helper Methods', () => {
    it('should check if field is invalid', () => {
      const fullNameControl = component.configForm.get('fullName');
      fullNameControl?.setValue('');
      fullNameControl?.markAsTouched();
      fullNameControl?.markAsDirty();

      expect(component.isFieldInvalid('fullName')).toBe(true);

      fullNameControl?.setValue('Valid Name');
      expect(component.isFieldInvalid('fullName')).toBe(false);
    });

    it('should check if field has specific error', () => {
      const emailControl = component.configForm.get('email');
      emailControl?.setValue('invalid-email');
      emailControl?.markAsTouched();
      emailControl?.markAsDirty();

      expect(component.hasError('email', 'email')).toBe(true);

      emailControl?.setValue('valid@email.com');
      expect(component.hasError('email', 'email')).toBe(false);
    });

    it('should navigate back', () => {
      component.goBack();

      expect(location.back).toHaveBeenCalled();
    });
  });
});
