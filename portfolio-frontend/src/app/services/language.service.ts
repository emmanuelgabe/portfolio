import { Injectable, signal, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LoggerService } from './logger.service';

export interface Language {
  code: string;
  label: string;
}

/**
 * Language service for managing application internationalization
 * Wraps ngx-translate with automatic language detection and persistence
 */
@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly logger = inject(LoggerService);

  private readonly STORAGE_KEY = 'preferred-language';
  private readonly DEFAULT_LANG = 'fr';
  private readonly SUPPORTED_LANGS = ['en', 'fr', 'es', 'pt', 'de', 'zh', 'ja', 'ru', 'ar', 'hi'];
  private readonly RTL_LANGS = ['ar'];

  readonly currentLang = signal<string>(this.DEFAULT_LANG);

  private readonly languages: Language[] = [
    { code: 'en', label: 'English' },
    { code: 'fr', label: 'Français' },
    { code: 'es', label: 'Español' },
    { code: 'pt', label: 'Português' },
    { code: 'de', label: 'Deutsch' },
    { code: 'zh', label: '中文' },
    { code: 'ja', label: '日本語' },
    { code: 'ru', label: 'Русский' },
    { code: 'ar', label: 'العربية' },
    { code: 'hi', label: 'हिन्दी' },
  ];

  /**
   * Initialize language service
   * Should be called once during application bootstrap
   */
  initialize(): void {
    const savedLang = localStorage.getItem(this.STORAGE_KEY);
    const browserLang = this.translate.getBrowserLang();

    let lang = this.DEFAULT_LANG;

    if (savedLang && this.SUPPORTED_LANGS.includes(savedLang)) {
      lang = savedLang;
    } else if (browserLang && this.SUPPORTED_LANGS.includes(browserLang)) {
      lang = browserLang;
    }

    this.setLanguage(lang);
    this.logger.info('[I18N] Language service initialized', { lang });
  }

  /**
   * Set the current language
   * @param lang Language code (e.g., 'fr', 'en')
   */
  setLanguage(lang: string): void {
    if (!this.SUPPORTED_LANGS.includes(lang)) {
      this.logger.warn('[I18N] Unsupported language requested', { lang });
      return;
    }

    this.translate.use(lang);
    this.currentLang.set(lang);
    localStorage.setItem(this.STORAGE_KEY, lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = this.RTL_LANGS.includes(lang) ? 'rtl' : 'ltr';

    this.logger.info('[I18N] Language changed', { lang });
  }

  /**
   * Get list of supported languages
   */
  getSupportedLanguages(): Language[] {
    return this.languages;
  }

  /**
   * Get current language code
   */
  getCurrentLanguage(): string {
    return this.currentLang();
  }

  /**
   * Check if a language is supported
   */
  isSupported(lang: string): boolean {
    return this.SUPPORTED_LANGS.includes(lang);
  }

  /**
   * Get translation for a key synchronously
   * Use only when translations are guaranteed to be loaded
   */
  instant(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }
}
