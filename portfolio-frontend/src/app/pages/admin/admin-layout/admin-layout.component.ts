import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../services/auth.service';
import { DemoModeService } from '../../../services/demo-mode.service';
import { User } from '../../../models/auth.model';
import { LanguageSelectorComponent } from '../../../components/shared/language-selector/language-selector.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, LanguageSelectorComponent],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss'],
})
export class AdminLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly demoModeService = inject(DemoModeService);

  currentUser: User | undefined;
  isSidebarCollapsed = false;

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser() ?? undefined;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
