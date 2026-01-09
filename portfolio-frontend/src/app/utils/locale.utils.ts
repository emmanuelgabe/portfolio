/**
 * Locale utilities for consistent date/time formatting across the application.
 */

const LOCALE_MAP: Record<string, string> = {
  fr: 'fr-FR',
  en: 'en-US',
  de: 'de-DE',
  es: 'es-ES',
  pt: 'pt-PT',
  ar: 'ar-SA',
  zh: 'zh-CN',
  ja: 'ja-JP',
  ru: 'ru-RU',
  hi: 'hi-IN',
};

/**
 * Get locale string from language code for Intl API
 * @param lang Language code (e.g., 'fr', 'en')
 * @returns Locale string (e.g., 'fr-FR', 'en-US')
 */
export function getLocaleFromLang(lang: string): string {
  return LOCALE_MAP[lang] || 'fr-FR';
}
