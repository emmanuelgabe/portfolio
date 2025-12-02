import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { DemoModeService } from '../../../services/demo-mode.service';

@Component({
  selector: 'app-demo-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './demo-layout.component.html',
  styleUrls: ['./demo-layout.component.scss'],
})
export class DemoLayoutComponent implements OnInit {
  private readonly demoModeService = inject(DemoModeService);
  private readonly router = inject(Router);

  isSidebarCollapsed = false;

  ngOnInit(): void {
    this.demoModeService.enableDemoMode();
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  returnToSite(): void {
    this.demoModeService.disableDemoMode();
    this.router.navigate(['/']);
  }
}
