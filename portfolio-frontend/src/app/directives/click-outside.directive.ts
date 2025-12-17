import {
  Directive,
  ElementRef,
  EventEmitter,
  Output,
  OnDestroy,
  NgZone,
  inject,
} from '@angular/core';
import { fromEvent, Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

/**
 * Directive to detect clicks outside of an element.
 * Emits an event when a click occurs outside the host element.
 * Uses RxJS for better performance (runs outside Angular zone).
 */
@Directive({
  selector: '[clickOutside]',
  standalone: true,
})
export class ClickOutsideDirective implements OnDestroy {
  @Output() clickOutside = new EventEmitter<void>();

  private readonly elementRef = inject(ElementRef);
  private readonly ngZone = inject(NgZone);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    this.ngZone.runOutsideAngular(() => {
      fromEvent<MouseEvent>(document, 'click')
        .pipe(
          filter((event) => {
            const target = event.target as HTMLElement;
            return target && !this.elementRef.nativeElement.contains(target);
          }),
          takeUntil(this.destroy$)
        )
        .subscribe(() => {
          this.ngZone.run(() => {
            this.clickOutside.emit();
          });
        });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
