import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MonthYearPickerComponent } from './month-year-picker.component';

describe('MonthYearPickerComponent', () => {
  let component: MonthYearPickerComponent;
  let fixture: ComponentFixture<MonthYearPickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MonthYearPickerComponent, ReactiveFormsModule, TranslateModule.forRoot()],
    }).compileComponents();

    fixture = TestBed.createComponent(MonthYearPickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ========== Initialization Tests ==========

  it('should_createComponent_when_instantiated', () => {
    expect(component).toBeTruthy();
  });

  it('should_initializeMonths_when_ngOnInitCalled', () => {
    // Arrange & Act
    component.ngOnInit();

    // Assert
    expect(component.months.length).toBe(12);
    expect(component.months[0].value).toBe(1);
    expect(component.months[11].value).toBe(12);
  });

  it('should_initializeYears_when_ngOnInitCalled', () => {
    // Arrange & Act
    component.ngOnInit();

    // Assert
    expect(component.years.length).toBeGreaterThan(0);
    expect(component.years[0]).toBeGreaterThanOrEqual(component.minYear);
  });

  // ========== ControlValueAccessor Tests ==========

  it('should_parseDate_when_writeValueCalledWithValidDate', () => {
    // Arrange
    const dateString = '2024-06-15';

    // Act
    component.writeValue(dateString);

    // Assert
    expect(component.selectedMonth).toBe(6);
    expect(component.selectedYear).toBe(2024);
  });

  it('should_clearSelection_when_writeValueCalledWithNull', () => {
    // Arrange
    component.selectedMonth = 5;
    component.selectedYear = 2024;

    // Act
    component.writeValue(null);

    // Assert
    expect(component.selectedMonth).toBeNull();
    expect(component.selectedYear).toBeNull();
  });

  it('should_clearSelection_when_writeValueCalledWithEmptyString', () => {
    // Arrange
    component.selectedMonth = 5;
    component.selectedYear = 2024;

    // Act
    component.writeValue('');

    // Assert
    expect(component.selectedMonth).toBeNull();
    expect(component.selectedYear).toBeNull();
  });

  it('should_setDisabled_when_setDisabledStateCalled', () => {
    // Arrange & Act
    component.setDisabledState(true);

    // Assert
    expect(component.disabled).toBe(true);
  });

  // ========== Value Emission Tests ==========

  it('should_emitIsoDate_when_monthAndYearSelected', () => {
    // Arrange
    let emittedValue = '';
    component.registerOnChange((value: string | null) => {
      emittedValue = value || '';
    });

    // Act
    component.onYearChange(2024);
    component.onMonthChange(3);

    // Assert
    expect(emittedValue).toBe('2024-03-01');
  });

  it('should_emitNull_when_onlyMonthSelected', () => {
    // Arrange
    let emittedValue: string | null = 'initial';
    component.registerOnChange((value: string | null) => {
      emittedValue = value;
    });

    // Act
    component.onMonthChange(3);

    // Assert
    expect(emittedValue).toBeNull();
  });

  it('should_emitNull_when_onlyYearSelected', () => {
    // Arrange
    let emittedValue: string | null = 'initial';
    component.registerOnChange((value: string | null) => {
      emittedValue = value;
    });

    // Act
    component.onYearChange(2024);

    // Assert
    expect(emittedValue).toBeNull();
  });

  it('should_padMonth_when_monthIsSingleDigit', () => {
    // Arrange
    let emittedValue = '';
    component.registerOnChange((value: string | null) => {
      emittedValue = value || '';
    });

    // Act
    component.selectedYear = 2024;
    component.onMonthChange(1);

    // Assert
    expect(emittedValue).toBe('2024-01-01');
  });

  // ========== Touch Handler Tests ==========

  it('should_callOnTouched_when_monthChanged', () => {
    // Arrange
    let touched = false;
    component.registerOnTouched(() => {
      touched = true;
    });

    // Act
    component.onMonthChange(5);

    // Assert
    expect(touched).toBe(true);
  });

  it('should_callOnTouched_when_yearChanged', () => {
    // Arrange
    let touched = false;
    component.registerOnTouched(() => {
      touched = true;
    });

    // Act
    component.onYearChange(2024);

    // Assert
    expect(touched).toBe(true);
  });
});
