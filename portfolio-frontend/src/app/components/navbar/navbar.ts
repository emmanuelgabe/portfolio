import { Component, HostListener, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly toastr = inject(ToastrService);

  isCollapsed = true;
  isScrolled = false;
  currentUser: User | null = null;

  constructor() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  toggleNavbar() {
    this.isCollapsed = !this.isCollapsed;
  }

  closeNavbar() {
    this.isCollapsed = true;
  }

  scrollToSection(sectionId: string) {
    this.closeNavbar();
    const element = document.getElementById(sectionId);
    if (element) {
      const navbarHeight = 76; // Height of fixed navbar
      const elementPosition = element.getBoundingClientRect().top + window.pageYOffset;
      const offsetPosition = elementPosition - navbarHeight;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth',
      });
    }
  }

  logout() {
    const username = this.currentUser?.username;
    this.authService.logout();
    this.closeNavbar();
    this.toastr.success(`À bientôt ${username} !`, 'Déconnexion réussie');
  }

  get isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }
}
