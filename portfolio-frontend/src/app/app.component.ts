import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { environment } from '../environments/environment';
import { NavbarComponent } from './components/navbar/navbar';
import { LoggerService } from './services/logger.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  private readonly logger = inject(LoggerService);
  private readonly router = inject(Router);

  version = environment.version;
  isAdminRoute = false;

  ngOnInit(): void {
    this.logger.info('[APP_INIT] Application started', { version: this.version });

    // Check initial route
    this.checkRoute(this.router.url);

    // Listen to route changes
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.checkRoute(event.urlAfterRedirects);
      });
  }

  private checkRoute(url: string): void {
    this.isAdminRoute = url.startsWith('/admin');
  }
}
