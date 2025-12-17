import { Directive, ElementRef, Input, OnInit, OnDestroy, inject, Renderer2 } from '@angular/core';

/**
 * Directive for lazy loading images using Intersection Observer.
 * Defers image loading until the image enters the viewport.
 *
 * Usage:
 * <img appLazyImage="path/to/image.jpg" [placeholder]="'placeholder.jpg'" alt="Description">
 */
@Directive({
  selector: '[appLazyImage]',
  standalone: true,
})
export class LazyImageDirective implements OnInit, OnDestroy {
  @Input('appLazyImage') imageSrc = '';
  @Input() placeholder =
    'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 200"%3E%3Crect fill="%23e9ecef" width="300" height="200"/%3E%3C/svg%3E';

  private readonly elementRef = inject(ElementRef);
  private readonly renderer = inject(Renderer2);
  private observer: IntersectionObserver | null = null;

  ngOnInit(): void {
    if (this.supportsIntersectionObserver()) {
      this.initLazyLoading();
    } else {
      this.loadImage();
    }
  }

  ngOnDestroy(): void {
    this.disconnectObserver();
  }

  private supportsIntersectionObserver(): boolean {
    return 'IntersectionObserver' in window;
  }

  private initLazyLoading(): void {
    const element = this.elementRef.nativeElement as HTMLImageElement;

    this.renderer.setAttribute(element, 'src', this.placeholder);
    this.renderer.addClass(element, 'lazy-image');
    this.renderer.addClass(element, 'lazy-image--loading');

    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.loadImage();
            this.disconnectObserver();
          }
        });
      },
      {
        rootMargin: '50px 0px',
        threshold: 0.01,
      }
    );

    this.observer.observe(element);
  }

  private loadImage(): void {
    const element = this.elementRef.nativeElement as HTMLImageElement;

    const img = new Image();
    img.onload = () => {
      this.renderer.setAttribute(element, 'src', this.imageSrc);
      this.renderer.removeClass(element, 'lazy-image--loading');
      this.renderer.addClass(element, 'lazy-image--loaded');
    };
    img.onerror = () => {
      this.renderer.removeClass(element, 'lazy-image--loading');
      this.renderer.addClass(element, 'lazy-image--error');
    };
    img.src = this.imageSrc;
  }

  private disconnectObserver(): void {
    if (this.observer) {
      this.observer.disconnect();
      this.observer = null;
    }
  }
}
