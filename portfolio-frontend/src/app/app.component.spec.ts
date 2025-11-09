import { TestBed, ComponentFixture } from '@angular/core/testing';
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
      imports: [AppComponent]
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
    // Spy on console.log
    const consoleSpy = spyOn(console, 'log');

    // Call ngOnInit
    component.ngOnInit();

    // Verify console.log was called with correct message
    expect(consoleSpy).toHaveBeenCalledWith(`Portfolio Application ${component.version}`);
  });

  it('should render title in h1 tag', () => {
    fixture.detectChanges();
    const h1 = compiled.querySelector('h1');

    expect(h1).toBeTruthy();
    expect(h1?.textContent).toContain('Portfolio Application');
  });

  it('should display version number', () => {
    fixture.detectChanges();
    const versionElement = compiled.querySelector('p strong');

    expect(versionElement).toBeTruthy();
    expect(versionElement?.textContent).toBe('Version:');
  });

  it('should render version value in template', () => {
    fixture.detectChanges();
    const content = compiled.textContent;

    expect(content).toContain(component.version);
  });

  it('should have container with text-center class', () => {
    fixture.detectChanges();
    const container = compiled.querySelector('.container .text-center');

    expect(container).toBeTruthy();
  });

  it('should have Bootstrap styling classes applied', () => {
    fixture.detectChanges();
    const container = compiled.querySelector('.container');
    const textCenter = compiled.querySelector('.text-center');
    const mt5 = compiled.querySelector('.mt-5');
    const mt4 = compiled.querySelector('.mt-4');

    expect(container).toBeTruthy();
    expect(textCenter).toBeTruthy();
    expect(mt5).toBeTruthy();
    expect(mt4).toBeTruthy();
  });

  it('should update version when environment changes', () => {
    const newVersion = '2.0.0';
    component.version = newVersion;
    fixture.detectChanges();

    const content = compiled.textContent;
    expect(content).toContain(newVersion);
  });

  it('should have exactly one h1 element', () => {
    fixture.detectChanges();
    const h1Elements = compiled.querySelectorAll('h1');

    expect(h1Elements.length).toBe(1);
  });

  it('should not have any error elements initially', () => {
    fixture.detectChanges();
    const errorElements = compiled.querySelectorAll('.error, .alert-danger');

    expect(errorElements.length).toBe(0);
  });
});