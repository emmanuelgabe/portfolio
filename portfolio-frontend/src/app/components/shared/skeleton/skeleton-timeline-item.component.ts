import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-timeline-item',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="timeline-item"
      [class.timeline-left]="position === 'left'"
      [class.timeline-right]="position === 'right'"
      role="status"
      aria-busy="true"
      aria-label="Loading experience"
    >
      <!-- Marker skeleton -->
      <div class="timeline-marker">
        <div class="skeleton-marker-icon skeleton--animated"></div>
      </div>

      <!-- Card skeleton -->
      <div class="timeline-card">
        <div class="timeline-card-header">
          <!-- Badge skeleton -->
          <div class="skeleton-badge skeleton--animated"></div>
          <!-- Role skeleton -->
          <div class="skeleton-role skeleton--animated"></div>
          <!-- Company skeleton -->
          <div class="skeleton-company skeleton--animated"></div>
          <!-- Date skeleton -->
          <div class="skeleton-date skeleton--animated"></div>
        </div>

        <div class="timeline-card-body">
          <!-- Description skeleton -->
          <div class="skeleton-text skeleton--animated"></div>
          <div class="skeleton-text skeleton--animated"></div>
          <div class="skeleton-text skeleton-text--short skeleton--animated"></div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .timeline-item {
        position: relative;
        margin-bottom: -50px;
        width: calc(50% - 40px);
      }

      .timeline-left {
        margin-left: 0;
        text-align: right;
      }

      .timeline-left .timeline-card {
        margin-right: 60px;
      }

      .timeline-left .timeline-marker {
        right: -15px;
      }

      .timeline-right {
        margin-left: auto;
        text-align: left;
      }

      .timeline-right .timeline-card {
        margin-left: 60px;
      }

      .timeline-right .timeline-marker {
        left: -15px;
      }

      .timeline-marker {
        position: absolute;
        top: 0;
        width: 50px;
        height: 50px;
        background: #fff;
        border: 3px solid #e9ecef;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .skeleton-marker-icon {
        width: 24px;
        height: 24px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .timeline-card {
        background: #fff;
        border: 1px solid #dee2e6;
        border-radius: 8px;
        padding: 20px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
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

      .skeleton-badge {
        height: 22px;
        width: 80px;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-role {
        height: 20px;
        width: 70%;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-company {
        height: 18px;
        width: 50%;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-date {
        height: 14px;
        width: 120px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .timeline-card-body {
        margin-top: 15px;
      }

      .skeleton-text {
        height: 14px;
        width: 100%;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-text--short {
        width: 70%;
      }

      /* Tablet responsive */
      @media (max-width: 992px) {
        .timeline-item {
          width: calc(100% - 70px);
          margin-left: 70px !important;
          text-align: left;
        }

        .timeline-left,
        .timeline-right {
          margin-left: 70px;
        }

        .timeline-left .timeline-card,
        .timeline-right .timeline-card {
          margin-left: 0;
          margin-right: 0;
          text-align: left;
        }

        .timeline-marker {
          width: 40px;
          height: 40px;
        }

        .timeline-left .timeline-marker,
        .timeline-right .timeline-marker {
          left: -42px;
          right: auto;
        }
      }

      /* Mobile responsive */
      @media (max-width: 576px) {
        .timeline-item {
          width: 100%;
          margin-left: 0 !important;
          margin-bottom: 20px;
        }

        .timeline-left,
        .timeline-right {
          margin-left: 0;
        }

        .timeline-marker {
          display: none;
        }

        .timeline-card {
          padding: 15px;
          text-align: left;
        }
      }
    `,
  ],
})
export class SkeletonTimelineItemComponent {
  @Input() position: 'left' | 'right' = 'left';
}
