import { Component, EventEmitter, inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { LoggerService } from '../../../services/logger.service';

/**
 * Reusable search input component with debounce functionality.
 * Emits search events after 300ms debounce and provides clear functionality.
 */
@Component({
  selector: 'app-search-input',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './search-input.component.html',
  styleUrls: ['./search-input.component.css'],
})
export class SearchInputComponent implements OnInit, OnDestroy {
  private readonly logger = inject(LoggerService);
  private readonly searchSubject = new Subject<string>();
  private searchSubscription: Subscription | undefined;

  @Input() placeholder = 'admin.common.search';
  @Input() minLength = 2;
  @Input() debounceMs = 300;
  @Input() loading = false;

  @Output() search = new EventEmitter<string>();
  @Output() clear = new EventEmitter<void>();

  searchQuery = '';

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(this.debounceMs), distinctUntilChanged())
      .subscribe((query) => {
        if (query.length >= this.minLength) {
          this.logger.debug('[SEARCH] Emitting search query', { query });
          this.search.emit(query);
        } else if (query.length === 0) {
          this.onClear();
        }
      });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  onInputChange(value: string): void {
    this.searchQuery = value;
    this.searchSubject.next(value.trim());
  }

  onClear(): void {
    this.searchQuery = '';
    this.logger.debug('[SEARCH] Search cleared');
    this.clear.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.onClear();
    }
  }
}
