import { TestBed } from '@angular/core/testing';
import { LanguageService } from './language.service';
import { LoggerService } from './logger.service';
import { TranslateService } from '@ngx-translate/core';

describe('LanguageService', () => {
  let service: LanguageService;
  let loggerSpy: jasmine.SpyObj<LoggerService>;
  let translateSpy: jasmine.SpyObj<TranslateService>;

  beforeEach(() => {
    loggerSpy = jasmine.createSpyObj('LoggerService', ['info', 'warn', 'error', 'debug']);
    translateSpy = jasmine.createSpyObj('TranslateService', ['use', 'getBrowserLang', 'instant']);
    translateSpy.getBrowserLang.and.returnValue('en');
    translateSpy.instant.and.callFake((key: string) => key);

    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        LanguageService,
        { provide: LoggerService, useValue: loggerSpy },
        { provide: TranslateService, useValue: translateSpy },
      ],
    });

    service = TestBed.inject(LanguageService);
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.lang = '';
    document.documentElement.dir = 'ltr';
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should_useSavedLanguage_when_initializeCalledWithStoredPreference', () => {
    // Arrange
    localStorage.setItem('preferred-language', 'es');

    // Act
    service.initialize();

    // Assert
    expect(translateSpy.use).toHaveBeenCalledWith('es');
    expect(service.getCurrentLanguage()).toBe('es');
  });

  it('should_useBrowserLanguage_when_initializeCalledWithNoStoredPreference', () => {
    // Arrange
    translateSpy.getBrowserLang.and.returnValue('de');

    // Act
    service.initialize();

    // Assert
    expect(translateSpy.use).toHaveBeenCalledWith('de');
    expect(service.getCurrentLanguage()).toBe('de');
  });

  it('should_useDefaultLanguage_when_initializeCalledWithUnsupportedBrowserLang', () => {
    // Arrange
    translateSpy.getBrowserLang.and.returnValue('xx');

    // Act
    service.initialize();

    // Assert
    expect(translateSpy.use).toHaveBeenCalledWith('fr');
    expect(service.getCurrentLanguage()).toBe('fr');
  });

  it('should_logInitialization_when_initializeCalled', () => {
    // Arrange / Act
    service.initialize();

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith(
      '[I18N] Language service initialized',
      jasmine.any(Object)
    );
  });

  // ========== setLanguage Tests ==========

  it('should_changeLanguage_when_setLanguageCalledWithSupportedLang', () => {
    // Arrange / Act
    service.setLanguage('pt');

    // Assert
    expect(translateSpy.use).toHaveBeenCalledWith('pt');
    expect(service.getCurrentLanguage()).toBe('pt');
    expect(localStorage.getItem('preferred-language')).toBe('pt');
  });

  it('should_setDocumentLang_when_setLanguageCalled', () => {
    // Arrange / Act
    service.setLanguage('ja');

    // Assert
    expect(document.documentElement.lang).toBe('ja');
  });

  it('should_setRTLDirection_when_setLanguageCalledWithArabic', () => {
    // Arrange / Act
    service.setLanguage('ar');

    // Assert
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('should_setLTRDirection_when_setLanguageCalledWithNonRTLLang', () => {
    // Arrange - set RTL first
    service.setLanguage('ar');
    expect(document.documentElement.dir).toBe('rtl');

    // Act
    service.setLanguage('en');

    // Assert
    expect(document.documentElement.dir).toBe('ltr');
  });

  it('should_notChangeLanguage_when_setLanguageCalledWithUnsupportedLang', () => {
    // Arrange
    service.setLanguage('fr');
    translateSpy.use.calls.reset();

    // Act
    service.setLanguage('xx');

    // Assert
    expect(translateSpy.use).not.toHaveBeenCalled();
    expect(service.getCurrentLanguage()).toBe('fr');
  });

  it('should_logWarning_when_setLanguageCalledWithUnsupportedLang', () => {
    // Arrange / Act
    service.setLanguage('xx');

    // Assert
    expect(loggerSpy.warn).toHaveBeenCalledWith('[I18N] Unsupported language requested', {
      lang: 'xx',
    });
  });

  it('should_logChange_when_setLanguageCalledWithSupportedLang', () => {
    // Arrange / Act
    service.setLanguage('zh');

    // Assert
    expect(loggerSpy.info).toHaveBeenCalledWith('[I18N] Language changed', { lang: 'zh' });
  });

  // ========== getSupportedLanguages Tests ==========

  it('should_returnAllLanguages_when_getSupportedLanguagesCalled', () => {
    // Arrange / Act
    const languages = service.getSupportedLanguages();

    // Assert
    expect(languages.length).toBe(10);
    expect(languages.map((l) => l.code)).toContain('en');
    expect(languages.map((l) => l.code)).toContain('fr');
    expect(languages.map((l) => l.code)).toContain('ar');
  });

  it('should_containLabelForEachLanguage_when_getSupportedLanguagesCalled', () => {
    // Arrange / Act
    const languages = service.getSupportedLanguages();
    const frLang = languages.find((l) => l.code === 'fr');

    // Assert
    expect(frLang).toBeDefined();
    expect(frLang?.label).toBe('Français');
  });

  // ========== isSupported Tests ==========

  it('should_returnTrue_when_isSupportedCalledWithSupportedLang', () => {
    // Arrange / Act / Assert
    expect(service.isSupported('en')).toBeTrue();
    expect(service.isSupported('fr')).toBeTrue();
    expect(service.isSupported('ar')).toBeTrue();
  });

  it('should_returnFalse_when_isSupportedCalledWithUnsupportedLang', () => {
    // Arrange / Act / Assert
    expect(service.isSupported('xx')).toBeFalse();
    expect(service.isSupported('it')).toBeFalse();
  });

  // ========== instant Tests ==========

  it('should_delegateToTranslate_when_instantCalled', () => {
    // Arrange
    translateSpy.instant.and.returnValue('Translated text');

    // Act
    const result = service.instant('test.key');

    // Assert
    expect(translateSpy.instant).toHaveBeenCalledWith('test.key', undefined);
    expect(result).toBe('Translated text');
  });

  it('should_passParams_when_instantCalledWithParams', () => {
    // Arrange
    const params = { name: 'Test' };

    // Act
    service.instant('test.key', params);

    // Assert
    expect(translateSpy.instant).toHaveBeenCalledWith('test.key', params);
  });

  // ========== getCurrentLanguage Tests ==========

  it('should_returnCurrentLang_when_getCurrentLanguageCalled', () => {
    // Arrange
    service.setLanguage('ru');

    // Act
    const result = service.getCurrentLanguage();

    // Assert
    expect(result).toBe('ru');
  });
});
