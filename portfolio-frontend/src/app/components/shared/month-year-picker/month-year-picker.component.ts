import { Component, forwardRef, inject, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { getLocaleFromLang } from '../../../utils/locale.utils';

/**
 * Reusable month/year picker component for date selection without day.
 * Implements ControlValueAccessor for seamless integration with reactive forms.
 * Outputs ISO date string format (YYYY-MM-01) for backend compatibility.
 */
@Component({
  selector: 'app-month-year-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './month-year-picker.component.html',
  styleUrls: ['./month-year-picker.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MonthYearPickerComponent),
      multi: true,
    },
  ],
})
export class MonthYearPickerComponent implements ControlValueAccessor, OnInit {
  @Input() disabled = false;
  @Input() minYear = 1970;
  @Input() maxYear = new Date().getFullYear() + 5;

  selectedMonth: number | null = null;
  selectedYear: number | null = null;

  months: { value: number; label: string }[] = [];
  years: number[] = [];

  private readonly translate = inject(TranslateService);

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  ngOnInit(): void {
    this.initMonths();
    this.initYears();
  }

  /**
   * Initialize month options with localized labels
   */
  private initMonths(): void {
    const currentLang = this.translate.currentLang || 'fr';
    const locale = getLocaleFromLang(currentLang);

    this.months = Array.from({ length: 12 }, (_, i) => {
      const date = new Date(2000, i, 1);
      const label = date.toLocaleDateString(locale, { month: 'long' });
      return {
        value: i + 1,
        label: label.charAt(0).toUpperCase() + label.slice(1),
      };
    });
  }

  /**
   * Initialize year options from minYear to maxYear (descending)
   */
  private initYears(): void {
    this.years = [];
    for (let year = this.maxYear; year >= this.minYear; year--) {
      this.years.push(year);
    }
  }

  /**
   * Handle month selection change
   */
  onMonthChange(month: number | null): void {
    this.selectedMonth = month;
    this.emitValue();
    this.onTouched();
  }

  /**
   * Handle year selection change
   */
  onYearChange(year: number | null): void {
    this.selectedYear = year;
    this.emitValue();
    this.onTouched();
  }

  /**
   * Emit combined date value in ISO format (YYYY-MM-01)
   */
  private emitValue(): void {
    if (this.selectedMonth && this.selectedYear) {
      const month = this.selectedMonth.toString().padStart(2, '0');
      const dateStr = `${this.selectedYear}-${month}-01`;
      this.onChange(dateStr);
    } else {
      this.onChange(null);
    }
  }

  // ControlValueAccessor implementation

  writeValue(value: string | null): void {
    if (value) {
      const date = new Date(value);
      if (!isNaN(date.getTime())) {
        this.selectedMonth = date.getMonth() + 1;
        this.selectedYear = date.getFullYear();
      }
    } else {
      this.selectedMonth = null;
      this.selectedYear = null;
    }
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
