import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ConnectivityService } from '../../services/connectivity.service';

@Component({
  selector: 'app-offline-banner',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './offline-banner.component.html',
  styleUrl: './offline-banner.component.css',
})
export class OfflineBannerComponent {
  private readonly connectivity = inject(ConnectivityService);

  readonly isOffline = this.connectivity.isOffline;
}
