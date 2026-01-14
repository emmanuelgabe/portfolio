import { TestBed } from '@angular/core/testing';
import { MarkdownService } from './markdown.service';

describe('MarkdownService', () => {
  let service: MarkdownService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarkdownService);
  });

  /**
   * Helper to extract HTML string from SafeHtml for testing
   */
  function getHtmlString(safeHtml: unknown): string {
    if (!safeHtml) return '';
    // SafeHtml has a changingThisBreaksApplicationSecurity property containing the raw HTML
    const safeValue = safeHtml as { changingThisBreaksApplicationSecurity?: string };
    return safeValue.changingThisBreaksApplicationSecurity || String(safeHtml);
  }

  // ========== Initialization Tests ==========

  it('should_createService_when_instantiated', () => {
    expect(service).toBeTruthy();
  });

  // ========== Empty Input Tests ==========

  it('should_returnEmptyString_when_toSafeHtmlCalledWithNull', () => {
    // Arrange & Act
    const result = service.toSafeHtml(null as unknown as string);

    // Assert
    expect(result).toBe('');
  });

  it('should_returnEmptyString_when_toSafeHtmlCalledWithEmptyString', () => {
    // Arrange & Act
    const result = service.toSafeHtml('');

    // Assert
    expect(result).toBe('');
  });

  it('should_returnEmptyString_when_toSafeHtmlCalledWithUndefined', () => {
    // Arrange & Act
    const result = service.toSafeHtml(undefined as unknown as string);

    // Assert
    expect(result).toBe('');
  });

  // ========== Basic Markdown Conversion Tests ==========

  it('should_convertBoldText_when_toSafeHtmlCalledWithBoldMarkdown', () => {
    // Arrange
    const markdown = '**bold text**';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<strong>bold text</strong>');
  });

  it('should_convertItalicText_when_toSafeHtmlCalledWithItalicMarkdown', () => {
    // Arrange
    const markdown = '*italic text*';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<em>italic text</em>');
  });

  it('should_convertHeading_when_toSafeHtmlCalledWithHeadingMarkdown', () => {
    // Arrange
    const markdown = '# Heading 1';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<h1');
    expect(htmlString).toContain('Heading 1');
  });

  it('should_convertUnorderedList_when_toSafeHtmlCalledWithListMarkdown', () => {
    // Arrange
    const markdown = '- item 1\n- item 2';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<ul>');
    expect(htmlString).toContain('<li>item 1</li>');
    expect(htmlString).toContain('<li>item 2</li>');
  });

  it('should_convertLink_when_toSafeHtmlCalledWithLinkMarkdown', () => {
    // Arrange
    const markdown = '[Google](https://google.com)';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<a href="https://google.com"');
    expect(htmlString).toContain('Google</a>');
  });

  it('should_convertBlockquote_when_toSafeHtmlCalledWithQuoteMarkdown', () => {
    // Arrange
    const markdown = '> This is a quote';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<blockquote>');
    expect(htmlString).toContain('This is a quote');
  });

  // ========== GFM (GitHub Flavored Markdown) Tests ==========

  it('should_convertLineBreaks_when_toSafeHtmlCalledWithNewlines', () => {
    // Arrange
    const markdown = 'line 1\nline 2';

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<br');
  });

  // ========== Security Tests (DOMPurify) ==========

  it('should_stripScriptTags_when_toSafeHtmlCalledWithXssPayload', () => {
    // Arrange
    const maliciousMarkdown = '<script>alert("xss")</script>Normal text';

    // Act
    const result = service.toSafeHtml(maliciousMarkdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).not.toContain('<script>');
    expect(htmlString).not.toContain('alert');
    expect(htmlString).toContain('Normal text');
  });

  it('should_stripOnClickHandlers_when_toSafeHtmlCalledWithEventHandler', () => {
    // Arrange
    const maliciousMarkdown = '<div onclick="alert(1)">Click me</div>';

    // Act
    const result = service.toSafeHtml(maliciousMarkdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).not.toContain('onclick');
    expect(htmlString).toContain('Click me');
  });

  it('should_stripJavascriptUrls_when_toSafeHtmlCalledWithJsLink', () => {
    // Arrange
    const maliciousMarkdown = '[Click](javascript:alert(1))';

    // Act
    const result = service.toSafeHtml(maliciousMarkdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).not.toContain('javascript:');
  });

  it('should_stripIframeTags_when_toSafeHtmlCalledWithIframe', () => {
    // Arrange
    const maliciousMarkdown = '<iframe src="https://evil.com"></iframe>Text';

    // Act
    const result = service.toSafeHtml(maliciousMarkdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).not.toContain('<iframe');
    expect(htmlString).toContain('Text');
  });

  // ========== Complex Markdown Tests ==========

  it('should_convertComplexMarkdown_when_toSafeHtmlCalledWithMixedContent', () => {
    // Arrange
    const markdown = `# Title

**Bold** and *italic* text.

- List item 1
- List item 2

> A quote

[Link](https://example.com)`;

    // Act
    const result = service.toSafeHtml(markdown);
    const htmlString = getHtmlString(result);

    // Assert
    expect(htmlString).toContain('<h1');
    expect(htmlString).toContain('<strong>Bold</strong>');
    expect(htmlString).toContain('<em>italic</em>');
    expect(htmlString).toContain('<ul>');
    expect(htmlString).toContain('<blockquote>');
    expect(htmlString).toContain('<a href="https://example.com"');
  });
});
