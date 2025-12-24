import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-skeleton-table-row',
  standalone: true,
  template: `
    <tr class="skeleton-table-row" role="status" aria-busy="true" aria-label="Loading row">
      @for (col of columns; track col) {
        <td>
          <div class="skeleton-cell skeleton--animated" [style.width]="col.width || '100%'"></div>
        </td>
      }
    </tr>
  `,
  styles: [
    `
      .skeleton-table-row td {
        padding: 12px;
        vertical-align: middle;
      }

      .skeleton-cell {
        height: 16px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton--animated {
        background: linear-gradient(90deg, #e9ecef 0%, #f8f9fa 50%, #e9ecef 100%);
        background-size: 200% 100%;
        animation: shimmer 1.5s ease-in-out infinite;
      }

      @keyframes shimmer {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }
    `,
  ],
})
export class SkeletonTableRowComponent {
  @Input() columns: { width?: string }[] = [
    { width: '30%' },
    { width: '40%' },
    { width: '20%' },
    { width: '10%' },
  ];
}
