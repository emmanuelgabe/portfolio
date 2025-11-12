import { Component, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavbarComponent {
  isCollapsed = true;
  isScrolled = false;

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
        behavior: 'smooth'
      });
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }
}
