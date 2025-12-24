import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LanguageService, Language } from '../../../services/language.service';
import { ClickOutsideDirective } from '../../../directives/click-outside.directive';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, ClickOutsideDirective],
  templateUrl: './language-selector.component.html',
  styleUrls: ['./language-selector.component.css'],
})
export class LanguageSelectorComponent {
  private readonly languageService = inject(LanguageService);

  languages: Language[] = this.languageService.getSupportedLanguages();
  isOpen = false;

  get currentLang(): string {
    return this.languageService.getCurrentLanguage();
  }

  get currentLangLabel(): string {
    const lang = this.languages.find((l) => l.code === this.currentLang);
    return lang?.label || this.currentLang.toUpperCase();
  }

  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  closeDropdown(): void {
    this.isOpen = false;
  }

  selectLanguage(lang: Language): void {
    this.languageService.setLanguage(lang.code);
    this.closeDropdown();
  }

  onKeydown(event: KeyboardEvent, lang: Language): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectLanguage(lang);
    }
  }
}
