import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LazyImageDirective } from './lazy-image.directive';

@Component({
  template: `
    <img
      appLazyImage="https://example.com/image.jpg"
      [placeholder]="placeholder"
      alt="Test image"
      class="test-image"
    />
  `,
  standalone: true,
  imports: [LazyImageDirective],
})
class TestHostComponent {
  placeholder = 'https://example.com/placeholder.jpg';
}

describe('LazyImageDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let imgElement: HTMLImageElement;
  let intersectionCallback: IntersectionObserverCallback;
  let mockObserverInstance: { observe: jasmine.Spy; disconnect: jasmine.Spy };
  let originalIntersectionObserver: typeof IntersectionObserver;

  beforeEach(() => {
    originalIntersectionObserver = window.IntersectionObserver;

    mockObserverInstance = {
      observe: jasmine.createSpy('observe'),
      disconnect: jasmine.createSpy('disconnect'),
    };

    (window as { IntersectionObserver: unknown }).IntersectionObserver =
      class MockIntersectionObserver {
        constructor(callback: IntersectionObserverCallback) {
          intersectionCallback = callback;
        }
        observe = mockObserverInstance.observe;
        disconnect = mockObserverInstance.disconnect;
        unobserve = jasmine.createSpy('unobserve');
        takeRecords = jasmine.createSpy('takeRecords').and.returnValue([]);
        root = null;
        rootMargin = '';
        thresholds = [];
      };

    TestBed.configureTestingModule({
      imports: [TestHostComponent],
    });

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    imgElement = fixture.nativeElement.querySelector('img');
  });

  afterEach(() => {
    (window as { IntersectionObserver: unknown }).IntersectionObserver =
      originalIntersectionObserver;
  });

  // ========== Initialization Tests ==========

  it('should create directive', () => {
    expect(imgElement).toBeTruthy();
  });

  it('should_setPlaceholder_when_directiveInitialized', () => {
    // Assert
    expect(imgElement.src).toContain('placeholder.jpg');
  });

  it('should_addLazyImageClass_when_directiveInitialized', () => {
    // Assert
    expect(imgElement.classList.contains('lazy-image')).toBeTrue();
  });

  it('should_addLoadingClass_when_directiveInitialized', () => {
    // Assert
    expect(imgElement.classList.contains('lazy-image--loading')).toBeTrue();
  });

  it('should_observeElement_when_directiveInitialized', () => {
    // Assert
    expect(mockObserverInstance.observe).toHaveBeenCalledWith(imgElement);
  });

  // ========== Intersection Observer Tests ==========

  it('should_disconnectObserver_when_elementIntersects', fakeAsync(() => {
    // Arrange
    const mockEntry: Partial<IntersectionObserverEntry> = {
      isIntersecting: true,
      target: imgElement,
    };

    // Act
    intersectionCallback([mockEntry as IntersectionObserverEntry], {} as IntersectionObserver);
    tick(100);

    // Assert
    expect(mockObserverInstance.disconnect).toHaveBeenCalled();
  }));

  it('should_notDisconnect_when_elementNotIntersecting', () => {
    // Arrange
    const mockEntry: Partial<IntersectionObserverEntry> = {
      isIntersecting: false,
      target: imgElement,
    };

    // Act
    intersectionCallback([mockEntry as IntersectionObserverEntry], {} as IntersectionObserver);

    // Assert
    expect(imgElement.src).toContain('placeholder.jpg');
    expect(mockObserverInstance.disconnect).not.toHaveBeenCalled();
  });

  // ========== Cleanup Tests ==========

  it('should_disconnectObserver_when_directiveDestroyed', () => {
    // Arrange / Act
    fixture.destroy();

    // Assert
    expect(mockObserverInstance.disconnect).toHaveBeenCalled();
  });
});

describe('LazyImageDirective default placeholder', () => {
  @Component({
    template: `<img appLazyImage="https://example.com/image.jpg" alt="Test image" />`,
    standalone: true,
    imports: [LazyImageDirective],
  })
  class TestHostWithDefaultPlaceholder {}

  let fixture: ComponentFixture<TestHostWithDefaultPlaceholder>;
  let imgElement: HTMLImageElement;
  let originalIntersectionObserver: typeof IntersectionObserver;

  beforeEach(() => {
    originalIntersectionObserver = window.IntersectionObserver;

    (window as { IntersectionObserver: unknown }).IntersectionObserver =
      class MockIntersectionObserver {
        constructor() {}
        observe = jasmine.createSpy('observe');
        disconnect = jasmine.createSpy('disconnect');
        unobserve = jasmine.createSpy('unobserve');
        takeRecords = jasmine.createSpy('takeRecords').and.returnValue([]);
        root = null;
        rootMargin = '';
        thresholds = [];
      };

    TestBed.configureTestingModule({
      imports: [TestHostWithDefaultPlaceholder],
    });

    fixture = TestBed.createComponent(TestHostWithDefaultPlaceholder);
    fixture.detectChanges();

    imgElement = fixture.nativeElement.querySelector('img');
  });

  afterEach(() => {
    (window as { IntersectionObserver: unknown }).IntersectionObserver =
      originalIntersectionObserver;
  });

  it('should_useDefaultPlaceholder_when_noPlaceholderProvided', () => {
    // Assert
    expect(imgElement.src).toContain('data:image/svg+xml');
  });
});
