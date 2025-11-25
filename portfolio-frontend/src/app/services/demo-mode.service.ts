import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class DemoModeService {
  private readonly router = inject(Router);
  private demoMode = signal<boolean>(false);

  isDemo(): boolean {
    return this.demoMode();
  }

  enableDemoMode(): void {
    this.demoMode.set(true);
  }

  disableDemoMode(): void {
    this.demoMode.set(false);
  }

  isDemoRoute(): boolean {
    return this.router.url.startsWith('/admindemo');
  }
}
