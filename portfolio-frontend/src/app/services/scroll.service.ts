import { Injectable } from '@angular/core';

/**
 * Service for scroll-related utilities.
 * Centralizes scroll logic to avoid duplication across components.
 */
@Injectable({
  providedIn: 'root',
})
export class ScrollService {
  private readonly NAVBAR_HEIGHT_DESKTOP = 76;
  private readonly NAVBAR_HEIGHT_MOBILE = 66;
  private readonly DESKTOP_BREAKPOINT = 1199;
  private readonly DEFAULT_MARGIN = 20;

  /**
   * Scroll to top of the page.
   * @param behavior Scroll behavior ('smooth' or 'auto')
   */
  scrollToTop(behavior: ScrollBehavior = 'smooth'): void {
    window.scrollTo({
      top: 0,
      behavior,
    });
  }

  /**
   * Scroll to an element by its ID, accounting for navbar height.
   * @param elementId The ID of the target element
   * @param behavior Scroll behavior ('smooth' or 'auto')
   * @param margin Additional margin below navbar
   * @returns True if element was found and scrolled to
   */
  scrollToElement(
    elementId: string,
    behavior: ScrollBehavior = 'smooth',
    margin: number = this.DEFAULT_MARGIN
  ): boolean {
    const element = document.getElementById(elementId);
    if (!element) {
      return false;
    }

    const navbarHeight = this.getNavbarHeight();
    const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
    const offsetPosition = elementPosition - navbarHeight - margin;

    window.scrollTo({
      top: offsetPosition,
      behavior,
    });

    return true;
  }

  /**
   * Scroll to a section, handling 'hero' as a special case (scroll to top).
   * @param sectionId The section ID ('hero' scrolls to top)
   * @param behavior Scroll behavior ('smooth' or 'auto')
   */
  scrollToSection(sectionId: string, behavior: ScrollBehavior = 'smooth'): void {
    if (sectionId === 'hero') {
      this.scrollToTop(behavior);
      return;
    }

    this.scrollToElement(sectionId, behavior);
  }

  /**
   * Get the current navbar height based on viewport width.
   * @returns Navbar height in pixels
   */
  getNavbarHeight(): number {
    return window.innerWidth > this.DESKTOP_BREAKPOINT
      ? this.NAVBAR_HEIGHT_DESKTOP
      : this.NAVBAR_HEIGHT_MOBILE;
  }
}
