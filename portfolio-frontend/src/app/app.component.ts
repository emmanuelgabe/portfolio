import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { environment } from '../environments/environment';
import { NavbarComponent } from './components/navbar/navbar';
import { LoggerService } from './services/logger.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private readonly logger = inject(LoggerService);
  version = environment.version;

  ngOnInit(): void {
    this.logger.info('[APP_INIT] Application started', { version: this.version });
  }
}
