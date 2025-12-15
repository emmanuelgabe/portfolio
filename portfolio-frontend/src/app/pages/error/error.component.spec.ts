import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ErrorComponent } from './error.component';
import { LoggerService } from '../../services/logger.service';

describe('ErrorComponent', () => {
  let component: ErrorComponent;
  let fixture: ComponentFixture<ErrorComponent>;
  let loggerSpy: jasmine.SpyObj<LoggerService>;

  const createComponent = (errorCode: string | null = '404') => {
    TestBed.configureTestingModule({
      imports: [ErrorComponent, TranslateModule.forRoot()],
      providers: [
        { provide: LoggerService, useValue: loggerSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ code: errorCode }),
            },
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ErrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    createComponent('404');
    expect(component).toBeTruthy();
  });

  it('should_loadConfig404_when_codeIs404', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    expect(component.config.code).toBe('404');
    expect(component.config.title).toBe('Page non trouvée');
  });

  it('should_loadConfig403_when_codeIs403', () => {
    // Arrange / Act
    createComponent('403');

    // Assert
    expect(component.config.code).toBe('403');
    expect(component.config.title).toBe('Accès refusé');
    expect(component.config.linkUrl).toBe('/login');
  });

  it('should_loadConfig429_when_codeIs429', () => {
    // Arrange / Act
    createComponent('429');

    // Assert
    expect(component.config.code).toBe('429');
    expect(component.config.title).toBe('Trop de requêtes');
  });

  it('should_loadConfig500_when_codeIs500', () => {
    // Arrange / Act
    createComponent('500');

    // Assert
    expect(component.config.code).toBe('500');
    expect(component.config.title).toBe('Erreur serveur');
  });

  it('should_loadConfigOffline_when_codeIsOffline', () => {
    // Arrange / Act
    createComponent('offline');

    // Assert
    expect(component.config.code).toBe('offline');
    expect(component.config.title).toBe('Connexion perdue');
  });

  it('should_loadDefaultConfig404_when_noCodeProvided', () => {
    // Arrange / Act
    createComponent(null);

    // Assert
    expect(component.config.code).toBe('404');
  });

  it('should_loadDefaultConfig404_when_invalidCodeProvided', () => {
    // Arrange / Act
    createComponent('999');

    // Assert
    expect(component.config.code).toBe('404');
  });

  // ========== Logging Tests ==========

  it('should_logWarning_when_errorPageDisplayed', () => {
    // Arrange / Act
    createComponent('500');

    // Assert
    expect(loggerSpy.warn).toHaveBeenCalledWith('[ERROR_PAGE] Error page displayed', {
      code: '500',
      title: 'Erreur serveur',
    });
  });

  // Note: reload() method cannot be tested as window.location.reload() crashes the test runner.
  // The method is simple enough (log + reload) that manual testing is sufficient.

  // ========== Configuration Tests ==========

  it('should_haveCorrectImage_when_configLoaded', () => {
    // Arrange / Act
    createComponent('403');

    // Assert
    expect(component.config.image).toBe('assets/images/errors/403.jpg');
  });

  it('should_haveCorrectLinkText_when_403Displayed', () => {
    // Arrange / Act
    createComponent('403');

    // Assert
    expect(component.config.linkText).toBe('Se connecter');
  });

  it('should_haveCorrectLinkText_when_404Displayed', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    expect(component.config.linkText).toBe("Retour à l'accueil");
  });

  // ========== Template Tests ==========

  it('should_displayErrorImage_when_rendered', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    const image = fixture.nativeElement.querySelector('.error-image');
    expect(image).toBeTruthy();
    expect(image.getAttribute('src')).toBe('assets/images/errors/404.jpg');
  });

  it('should_displayErrorTitle_when_rendered', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    const title = fixture.nativeElement.querySelector('.error-title');
    expect(title.textContent).toContain('Page non trouvée');
  });

  it('should_displayErrorMessage_when_rendered', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    const message = fixture.nativeElement.querySelector('.error-message');
    expect(message.textContent).toContain('existe pas');
  });

  it('should_displayPrimaryButton_when_rendered', () => {
    // Arrange / Act
    createComponent('404');

    // Assert
    const button = fixture.nativeElement.querySelector('.btn-primary');
    expect(button).toBeTruthy();
  });
});
