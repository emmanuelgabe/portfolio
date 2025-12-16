import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FooterComponent } from './footer.component';
import { SiteConfigurationService } from '../../services/site-configuration.service';
import { CvService } from '../../services/cv.service';
import { LoggerService } from '../../services/logger.service';
import { ToastrService } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { SiteConfigurationResponse } from '../../models/site-configuration.model';
import { CvResponse } from '../../models/cv.model';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;
  let siteConfigServiceSpy: jasmine.SpyObj<SiteConfigurationService>;
  let cvServiceSpy: jasmine.SpyObj<CvService>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const mockSiteConfig: SiteConfigurationResponse = {
    id: 1,
    fullName: 'John Doe',
    email: 'john@example.com',
    heroTitle: 'Welcome',
    heroDescription: 'Description',
    siteTitle: 'Site Title',
    seoDescription: 'SEO Description',
    profileImageUrl: '/images/profile.jpg',
    githubUrl: 'https://github.com/johndoe',
    linkedinUrl: 'https://linkedin.com/in/johndoe',
    createdAt: '2024-01-01T12:00:00',
    updatedAt: '2024-01-01T12:00:00',
  };

  const mockCv: CvResponse = {
    id: 1,
    fileName: 'cv_john_doe.pdf',
    originalFileName: 'CV_John_Doe.pdf',
    fileUrl: '/uploads/cv/cv_john_doe.pdf',
    fileSize: 102400,
    uploadedAt: '2024-01-01T12:00:00',
    current: true,
  };

  beforeEach(async () => {
    siteConfigServiceSpy = jasmine.createSpyObj('SiteConfigurationService', [
      'getSiteConfiguration',
    ]);
    cvServiceSpy = jasmine.createSpyObj('CvService', ['getCurrentCv', 'downloadCv']);
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'error', 'warn', 'debug']);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['info', 'error', 'success', 'warning']);

    siteConfigServiceSpy.getSiteConfiguration.and.returnValue(of(mockSiteConfig));
    cvServiceSpy.getCurrentCv.and.returnValue(of(mockCv));

    await TestBed.configureTestingModule({
      imports: [FooterComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: SiteConfigurationService, useValue: siteConfigServiceSpy },
        { provide: CvService, useValue: cvServiceSpy },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
  });

  // ========== Initialization Tests ==========

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should_initializeWithDefaultValues_when_created', () => {
    // Arrange / Act - component is created

    // Assert
    expect(component.githubUrl).toBe('https://github.com/emmanuelgabe');
    expect(component.linkedinUrl).toBe('https://linkedin.com/in/egabe');
    expect(component.contactEmail).toBe('contact@emmanuelgabe.com');
    expect(component.fullName).toBe('Emmanuel Gabe');
  });

  it('should_setCurrentYear_when_created', () => {
    // Arrange / Act - component is created
    const currentYear = new Date().getFullYear();

    // Assert
    expect(component.currentYear).toBe(currentYear);
  });

  it('should_haveVersion_when_created', () => {
    // Arrange / Act - component is created

    // Assert
    expect(component.version).toBeDefined();
  });

  // ========== ngOnInit Tests ==========

  it('should_loadSiteConfiguration_when_ngOnInitCalled', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(siteConfigServiceSpy.getSiteConfiguration).toHaveBeenCalled();
  }));

  it('should_loadCv_when_ngOnInitCalled', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(cvServiceSpy.getCurrentCv).toHaveBeenCalled();
  }));

  it('should_updateComponentValues_when_siteConfigLoaded', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.fullName).toBe('John Doe');
    expect(component.githubUrl).toBe('https://github.com/johndoe');
    expect(component.linkedinUrl).toBe('https://linkedin.com/in/johndoe');
    expect(component.contactEmail).toBe('john@example.com');
  }));

  it('should_setCurrentCv_when_cvLoaded', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.currentCv).toEqual(mockCv);
  }));

  // ========== Site Configuration Error Handling Tests ==========

  it('should_logError_when_siteConfigLoadFails', fakeAsync(() => {
    // Arrange
    const error = { message: 'Network error' };
    siteConfigServiceSpy.getSiteConfiguration.and.returnValue(throwError(() => error));

    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(loggerSpy.error).toHaveBeenCalledWith(
      '[HTTP_ERROR] Failed to load site configuration for footer',
      jasmine.objectContaining({ error: 'Network error' })
    );
  }));

  it('should_keepDefaultValues_when_siteConfigLoadFails', fakeAsync(() => {
    // Arrange
    siteConfigServiceSpy.getSiteConfiguration.and.returnValue(
      throwError(() => new Error('Failed'))
    );

    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.fullName).toBe('Emmanuel Gabe');
    expect(component.githubUrl).toBe('https://github.com/emmanuelgabe');
  }));

  // ========== CV Error Handling Tests ==========

  it('should_logError_when_cvLoadFails', fakeAsync(() => {
    // Arrange
    const error = { status: 404, message: 'CV not found' };
    cvServiceSpy.getCurrentCv.and.returnValue(throwError(() => error));

    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(loggerSpy.error).toHaveBeenCalledWith(
      '[HTTP_ERROR] Failed to load CV for footer',
      jasmine.objectContaining({ status: 404 })
    );
  }));

  it('should_setCurrentCvToUndefined_when_cvLoadFails', fakeAsync(() => {
    // Arrange
    cvServiceSpy.getCurrentCv.and.returnValue(throwError(() => new Error('Failed')));

    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.currentCv).toBeUndefined();
  }));

  it('should_setCurrentCvToUndefined_when_noCvExists', fakeAsync(() => {
    // Arrange
    cvServiceSpy.getCurrentCv.and.returnValue(of(null as unknown as CvResponse));

    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.currentCv).toBeUndefined();
  }));

  // ========== downloadCv Tests ==========

  it('should_callCvServiceDownload_when_downloadCvCalledWithCv', fakeAsync(() => {
    // Arrange
    fixture.detectChanges();
    tick();

    // Act
    component.downloadCv();

    // Assert
    expect(cvServiceSpy.downloadCv).toHaveBeenCalled();
  }));

  it('should_logUserAction_when_downloadCvCalled', fakeAsync(() => {
    // Arrange
    fixture.detectChanges();
    tick();

    // Act
    component.downloadCv();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[USER_ACTION] User clicked download CV button from footer'
    );
  }));

  it('should_showToast_when_downloadCvCalledWithoutCv', fakeAsync(() => {
    // Arrange
    cvServiceSpy.getCurrentCv.and.returnValue(of(null as unknown as CvResponse));
    fixture.detectChanges();
    tick();

    // Act
    component.downloadCv();

    // Assert
    expect(toastrSpy.info).toHaveBeenCalledWith('cv.notAvailable');
    expect(cvServiceSpy.downloadCv).not.toHaveBeenCalled();
  }));

  it('should_notLogUserAction_when_downloadCvCalledWithoutCv', fakeAsync(() => {
    // Arrange
    cvServiceSpy.getCurrentCv.and.returnValue(of(null as unknown as CvResponse));
    fixture.detectChanges();
    tick();
    loggerSpy.info.calls.reset();

    // Act
    component.downloadCv();

    // Assert
    expect(loggerSpy.info).not.toHaveBeenCalled();
  }));

  // ========== Template Rendering Tests ==========

  it('should_renderFooter_when_componentInitialized', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();

    // Assert
    const footerElement = fixture.nativeElement.querySelector('footer');
    expect(footerElement).toBeTruthy();
  }));

  it('should_displayFullName_when_rendered', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const footerText = fixture.nativeElement.textContent;
    expect(footerText).toContain('John Doe');
  }));

  it('should_displayCurrentYear_when_rendered', fakeAsync(() => {
    // Arrange / Act
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    // Assert
    const footerText = fixture.nativeElement.textContent;
    expect(footerText).toContain(new Date().getFullYear().toString());
  }));
});
