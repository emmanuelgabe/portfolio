import {
  Directive,
  ElementRef,
  Input,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  inject,
  Renderer2,
} from '@angular/core';

/**
 * Directive for loading images that are being processed asynchronously.
 * Shows a loading spinner and retries loading with exponential backoff.
 *
 * Usage:
 * <img [appAsyncImage]="imageUrl" [placeholder]="'assets/images/placeholder.svg'" alt="Description">
 */
@Directive({
  selector: '[appAsyncImage]',
  standalone: true,
})
export class AsyncImageDirective implements OnInit, OnDestroy, OnChanges {
  @Input('appAsyncImage') imageSrc = '';
  @Input() placeholder = 'assets/images/project-placeholder.svg';
  @Input() maxRetries = 5;
  @Input() initialDelay = 300;

  private readonly elementRef = inject(ElementRef);
  private readonly renderer = inject(Renderer2);
  private retryCount = 0;
  private retryTimeout: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  ngOnInit(): void {
    this.setupElement();
    this.loadImage();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['imageSrc'] && !changes['imageSrc'].firstChange) {
      this.resetState();
      this.loadImage();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.clearRetryTimeout();
  }

  private setupElement(): void {
    const element = this.elementRef.nativeElement as HTMLImageElement;
    this.renderer.addClass(element, 'async-image');
    this.renderer.addClass(element, 'async-image--loading');
    this.renderer.setAttribute(element, 'src', this.placeholder);
  }

  private loadImage(): void {
    if (this.destroyed || !this.imageSrc) {
      return;
    }

    const element = this.elementRef.nativeElement as HTMLImageElement;
    const img = new Image();

    img.onload = () => {
      if (this.destroyed) return;
      this.renderer.setAttribute(element, 'src', this.imageSrc);
      this.renderer.removeClass(element, 'async-image--loading');
      this.renderer.removeClass(element, 'async-image--error');
      this.renderer.addClass(element, 'async-image--loaded');
    };

    img.onerror = () => {
      if (this.destroyed) return;
      this.handleError();
    };

    // Add cache-buster for retries
    const url =
      this.retryCount > 0
        ? `${this.imageSrc}${this.imageSrc.includes('?') ? '&' : '?'}t=${Date.now()}`
        : this.imageSrc;

    img.src = url;
  }

  private handleError(): void {
    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      const delay = this.initialDelay * Math.pow(1.5, this.retryCount - 1);

      this.retryTimeout = setTimeout(() => {
        this.loadImage();
      }, delay);
    } else {
      this.showPlaceholder();
    }
  }

  private showPlaceholder(): void {
    const element = this.elementRef.nativeElement as HTMLImageElement;
    this.renderer.setAttribute(element, 'src', this.placeholder);
    this.renderer.removeClass(element, 'async-image--loading');
    this.renderer.addClass(element, 'async-image--error');
  }

  private resetState(): void {
    this.clearRetryTimeout();
    this.retryCount = 0;
    const element = this.elementRef.nativeElement as HTMLImageElement;
    this.renderer.removeClass(element, 'async-image--loaded');
    this.renderer.removeClass(element, 'async-image--error');
    this.renderer.addClass(element, 'async-image--loading');
  }

  private clearRetryTimeout(): void {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
      this.retryTimeout = null;
    }
  }
}
