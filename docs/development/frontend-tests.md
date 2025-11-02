# Frontend Testing Guide

**Document Type:** Technical Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active

---

## Table of Contents

1. [Overview](#1-overview)
2. [Test Structure](#2-test-structure)
3. [Test Implementation](#3-test-implementation)
4. [Configuration](#4-configuration)

---

## 1. Overview

Frontend tests use Jasmine and Karma to verify Angular component initialization, rendering, and behavior.

**Test Coverage:**
- AppComponent: 11 tests

---

## 2. Test Structure

```
portfolio-frontend/src/app/
└── app.component.spec.ts
```

---

## 3. Test Implementation

### 3.1 AppComponentTest

**Location:** `portfolio-frontend/src/app/app.component.spec.ts`

**Purpose:** Validates main application component initialization and rendering

**Test Pattern Example:**
```typescript
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

  it('should initialize and log version on ngOnInit', () => {
    const consoleSpy = spyOn(console, 'log');
    component.ngOnInit();
    expect(consoleSpy).toHaveBeenCalledWith(`Portfolio Application ${component.version}`);
  });

  it('should render title in h1 tag', () => {
    fixture.detectChanges();
    const h1 = compiled.querySelector('h1');
    expect(h1?.textContent).toContain('Portfolio Application');
  });
});
```

**Key Test Cases:**
- Component creation
- Version initialization
- Lifecycle hooks (ngOnInit)
- Template rendering (title, version)
- CSS classes
- DOM structure
- Initial state

---

## 4. Configuration

### 4.1 Test Configuration Files

**karma.conf.js:**
```javascript
module.exports = function(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    }
  });
};
```

**tsconfig.spec.json:**
```json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "outDir": "./out-tsc/spec",
    "types": ["jasmine"]
  },
  "include": ["src/**/*.spec.ts"]
}
```

### 4.2 Angular Test Configuration

Ensure `angular.json` includes:

```json
{
  "projects": {
    "portfolio-frontend": {
      "architect": {
        "test": {
          "builder": "@angular-devkit/build-angular:karma",
          "options": {
            "karmaConfig": "karma.conf.js"
          }
        }
      }
    }
  }
}
```

### 4.3 Spying on Dependencies

Use Jasmine spies to verify external calls:

```typescript
const consoleSpy = spyOn(console, 'log');
component.ngOnInit();
expect(consoleSpy).toHaveBeenCalled();
```

---

## Change History

| Version | Date       | Changes |
|---------|------------|---------|
| 1.0.0   | 2025-11-09 | Initial release |

---

**Document Type:** Technical Guide
**Version:** 1.0.0
**Last Updated:** 2025-11-09
**Status:** Active
