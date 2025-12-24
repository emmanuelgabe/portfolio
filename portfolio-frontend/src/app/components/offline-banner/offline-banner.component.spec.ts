import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { OfflineBannerComponent } from './offline-banner.component';
import { ConnectivityService } from '../../services/connectivity.service';

describe('OfflineBannerComponent', () => {
  let component: OfflineBannerComponent;
  let fixture: ComponentFixture<OfflineBannerComponent>;
  let mockConnectivityService: jasmine.SpyObj<ConnectivityService>;
  let isOfflineSignal: ReturnType<typeof signal<boolean>>;

  beforeEach(() => {
    isOfflineSignal = signal(false);

    mockConnectivityService = jasmine.createSpyObj('ConnectivityService', [], {
      isOffline: isOfflineSignal,
      isOnline: signal(true),
    });

    TestBed.configureTestingModule({
      imports: [OfflineBannerComponent, TranslateModule.forRoot()],
      providers: [{ provide: ConnectivityService, useValue: mockConnectivityService }],
    });

    fixture = TestBed.createComponent(OfflineBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  // ========== Display Tests ==========

  it('should_hideBanner_when_deviceIsOnline', () => {
    // Arrange
    isOfflineSignal.set(false);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner).toBeFalsy();
  });

  it('should_showBanner_when_deviceIsOffline', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner).toBeTruthy();
  });

  it('should_reactToConnectivityChanges_when_statusChanges', () => {
    // Arrange - start online
    isOfflineSignal.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.offline-banner')).toBeFalsy();

    // Act - go offline
    isOfflineSignal.set(true);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.offline-banner')).toBeTruthy();

    // Act - go back online
    isOfflineSignal.set(false);
    fixture.detectChanges();

    // Assert
    expect(fixture.nativeElement.querySelector('.offline-banner')).toBeFalsy();
  });

  // ========== Accessibility Tests ==========

  it('should_haveRoleAlert_when_bannerDisplayed', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner.getAttribute('role')).toBe('alert');
  });

  it('should_haveAriaLiveAssertive_when_bannerDisplayed', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner.getAttribute('aria-live')).toBe('assertive');
  });

  it('should_haveAriaAtomicTrue_when_bannerDisplayed', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner.getAttribute('aria-atomic')).toBe('true');
  });

  it('should_haveAriaHiddenOnIcon_when_bannerDisplayed', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const icon = fixture.nativeElement.querySelector('.bi-wifi-off');
    expect(icon.getAttribute('aria-hidden')).toBe('true');
  });

  // ========== Content Tests ==========

  it('should_displayOfflineMessage_when_offline', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const banner = fixture.nativeElement.querySelector('.offline-banner');
    expect(banner.textContent).toContain('connectivity.offline');
  });

  it('should_displayWifiOffIcon_when_offline', () => {
    // Arrange
    isOfflineSignal.set(true);

    // Act
    fixture.detectChanges();

    // Assert
    const icon = fixture.nativeElement.querySelector('.bi-wifi-off');
    expect(icon).toBeTruthy();
  });
});
