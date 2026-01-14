import { Injectable, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

/**
 * Service for client-side Markdown rendering.
 * Uses 'marked' library for Markdown to HTML conversion.
 * Uses DOMPurify to sanitize HTML and prevent XSS attacks.
 */
@Injectable({
  providedIn: 'root',
})
export class MarkdownService {
  private readonly sanitizer = inject(DomSanitizer);

  private static domPurifyConfigured = false;

  constructor() {
    this.configureMarked();
    this.configureDOMPurify();
  }

  /**
   * Configure marked options for GFM rendering
   */
  private configureMarked(): void {
    marked.setOptions({
      breaks: true,
      gfm: true,
    });
  }

  /**
   * Configure DOMPurify to allow safe style attributes (text-align)
   * Uses static flag to prevent multiple hook registrations
   */
  private configureDOMPurify(): void {
    if (MarkdownService.domPurifyConfigured) {
      return;
    }

    // Allow style attribute on div elements for text alignment
    DOMPurify.addHook('uponSanitizeAttribute', (node, data) => {
      if (data.attrName === 'style') {
        // Only allow text-align property
        const allowedStyles = data.attrValue
          .split(';')
          .map((s) => s.trim())
          .filter((s) => s.startsWith('text-align:'))
          .join('; ');
        data.attrValue = allowedStyles;
      }
    });

    MarkdownService.domPurifyConfigured = true;
  }

  /**
   * Convert Markdown to safe HTML for template binding.
   * HTML is sanitized with DOMPurify to prevent XSS attacks.
   * @param markdown Raw markdown string
   * @returns SafeHtml for use with [innerHTML]
   */
  toSafeHtml(markdown: string): SafeHtml {
    if (!markdown) {
      return '';
    }
    const rawHtml = marked.parse(markdown) as string;
    const cleanHtml = DOMPurify.sanitize(rawHtml, {
      ADD_ATTR: ['style'],
      ADD_TAGS: ['div'],
    });
    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml);
  }
}
