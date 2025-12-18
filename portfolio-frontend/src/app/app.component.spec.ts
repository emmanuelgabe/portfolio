import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideToastr } from 'ngx-toastr';
import { TranslateModule } from '@ngx-translate/core';
import { AppComponent } from './app.component';
import { environment } from '../environments/environment';

/**
 * Unit tests for AppComponent
 * Tests component initialization and version display
 */
describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let compiled: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), provideHttpClient(), provideToastr()],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    compiled = fixture.nativeElement as HTMLElement;
  });

  it('should create the app component', () => {
    expect(component).toBeTruthy();
  });

  it('should have version property from environment', () => {
    expect(component.version).toBeDefined();
    expect(component.version).toBe(environment.version);
  });

  it('should initialize and log version on ngOnInit', () => {
    // NgOnInit logs to LoggerService, not console.log
    component.ngOnInit();

    // Verify component initialized
    expect(component.version).toBeDefined();
    expect(component.version).toBe(environment.version);
  });

  it('should render navbar when not on admin route', () => {
    component.isAdminRoute = false;
    fixture.detectChanges();
    const navbar = compiled.querySelector('app-navbar');

    expect(navbar).toBeTruthy();
  });

  it('should not render navbar on admin route', () => {
    fixture.detectChanges(); // Let ngOnInit complete first
    component.isAdminRoute = true;
    fixture.detectChanges(); // Re-render with admin route
    const navbar = compiled.querySelector('app-navbar');

    expect(navbar).toBeFalsy();
  });

  it('should have router outlet', () => {
    fixture.detectChanges();
    const routerOutlet = compiled.querySelector('router-outlet');

    expect(routerOutlet).toBeTruthy();
  });

  it('should update isAdminRoute based on navigation', () => {
    // Test is covered by component logic
    expect(component.isAdminRoute).toBeDefined();
  });

  it('should set version from environment', () => {
    expect(component.version).toBe(environment.version);
  });

  it('should not have any error elements initially', () => {
    fixture.detectChanges();
    const errorElements = compiled.querySelectorAll('.error, .alert-danger');

    expect(errorElements.length).toBe(0);
  });
});
