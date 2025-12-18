import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { DemoModeService } from './demo-mode.service';

describe('DemoModeService', () => {
  let service: DemoModeService;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', [], { url: '/admin/projects' });

    TestBed.configureTestingModule({
      providers: [DemoModeService, { provide: Router, useValue: routerSpy }],
    });

    service = TestBed.inject(DemoModeService);
  });

  // ========== Initialization Tests ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should_initializeWithDemoModeDisabled_when_created', () => {
    // Arrange / Act - service is created in beforeEach

    // Assert
    expect(service.isDemo()).toBeFalse();
  });

  // ========== isDemo Tests ==========

  it('should_returnFalse_when_isDemoCalledAndDemoModeDisabled', () => {
    // Arrange - default state is disabled

    // Act
    const result = service.isDemo();

    // Assert
    expect(result).toBeFalse();
  });

  it('should_returnTrue_when_isDemoCalledAndDemoModeEnabled', () => {
    // Arrange
    service.enableDemoMode();

    // Act
    const result = service.isDemo();

    // Assert
    expect(result).toBeTrue();
  });

  // ========== enableDemoMode Tests ==========

  it('should_enableDemoMode_when_enableDemoModeCalled', () => {
    // Arrange
    expect(service.isDemo()).toBeFalse();

    // Act
    service.enableDemoMode();

    // Assert
    expect(service.isDemo()).toBeTrue();
  });

  it('should_remainEnabled_when_enableDemoModeCalledMultipleTimes', () => {
    // Arrange / Act
    service.enableDemoMode();
    service.enableDemoMode();
    service.enableDemoMode();

    // Assert
    expect(service.isDemo()).toBeTrue();
  });

  // ========== disableDemoMode Tests ==========

  it('should_disableDemoMode_when_disableDemoModeCalled', () => {
    // Arrange
    service.enableDemoMode();
    expect(service.isDemo()).toBeTrue();

    // Act
    service.disableDemoMode();

    // Assert
    expect(service.isDemo()).toBeFalse();
  });

  it('should_remainDisabled_when_disableDemoModeCalledMultipleTimes', () => {
    // Arrange / Act
    service.disableDemoMode();
    service.disableDemoMode();
    service.disableDemoMode();

    // Assert
    expect(service.isDemo()).toBeFalse();
  });

  it('should_toggleCorrectly_when_enableAndDisableCalledAlternately', () => {
    // Arrange / Act / Assert
    expect(service.isDemo()).toBeFalse();

    service.enableDemoMode();
    expect(service.isDemo()).toBeTrue();

    service.disableDemoMode();
    expect(service.isDemo()).toBeFalse();

    service.enableDemoMode();
    expect(service.isDemo()).toBeTrue();
  });

  // ========== isDemoRoute Tests ==========

  it('should_returnFalse_when_isDemoRouteCalledAndNotOnDemoRoute', () => {
    // Arrange - router url is '/admin/projects'

    // Act
    const result = service.isDemoRoute();

    // Assert
    expect(result).toBeFalse();
  });

  it('should_returnTrue_when_isDemoRouteCalledAndOnDemoRoute', () => {
    // Arrange
    Object.defineProperty(routerSpy, 'url', { get: () => '/admindemo/projects' });

    // Act
    const result = service.isDemoRoute();

    // Assert
    expect(result).toBeTrue();
  });

  it('should_returnTrue_when_isDemoRouteCalledAndOnNestedDemoRoute', () => {
    // Arrange
    Object.defineProperty(routerSpy, 'url', { get: () => '/admindemo/projects/1/edit' });

    // Act
    const result = service.isDemoRoute();

    // Assert
    expect(result).toBeTrue();
  });

  it('should_returnFalse_when_isDemoRouteCalledAndOnSimilarNonDemoRoute', () => {
    // Arrange
    Object.defineProperty(routerSpy, 'url', { get: () => '/admin-demo-page' });

    // Act
    const result = service.isDemoRoute();

    // Assert
    expect(result).toBeFalse();
  });

  it('should_returnTrue_when_isDemoRouteCalledAndOnExactDemoRoute', () => {
    // Arrange
    Object.defineProperty(routerSpy, 'url', { get: () => '/admindemo' });

    // Act
    const result = service.isDemoRoute();

    // Assert
    expect(result).toBeTrue();
  });
});
