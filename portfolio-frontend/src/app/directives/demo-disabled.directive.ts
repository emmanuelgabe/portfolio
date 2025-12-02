import { Directive, ElementRef, inject, OnInit, Renderer2 } from '@angular/core';
import { DemoModeService } from '../services/demo-mode.service';

/**
 * Directive that applies visual disabled state to elements in demo mode.
 * Reduces opacity and adds a tooltip when demo mode is active.
 *
 * Usage: <button appDemoDisabled>Action</button>
 */
@Directive({
  selector: '[appDemoDisabled]',
  standalone: true,
})
export class DemoDisabledDirective implements OnInit {
  private readonly el = inject(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly demoModeService = inject(DemoModeService);

  ngOnInit(): void {
    if (this.demoModeService.isDemo()) {
      this.renderer.setStyle(this.el.nativeElement, 'opacity', '0.65');
      this.renderer.setAttribute(
        this.el.nativeElement,
        'title',
        'Action desactivee en mode demonstration'
      );
    }
  }
}
