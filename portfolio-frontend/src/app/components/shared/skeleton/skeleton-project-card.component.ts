import { Component } from '@angular/core';

@Component({
  selector: 'app-skeleton-project-card',
  standalone: true,
  template: `
    <div
      class="card h-100 shadow-sm skeleton-project-card"
      role="status"
      aria-busy="true"
      aria-label="Loading project"
    >
      <!-- Image skeleton -->
      <div class="skeleton-image skeleton--animated"></div>

      <div class="card-body d-flex flex-column">
        <!-- Title skeleton -->
        <div class="skeleton-title skeleton--animated"></div>

        <!-- Description skeleton (3 lines) -->
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton-text--short skeleton--animated"></div>

        <!-- Tags skeleton -->
        <div class="skeleton-tags">
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton-badge--short skeleton--animated"></span>
        </div>

        <!-- Action skeleton -->
        <div class="skeleton-actions">
          <div class="skeleton-link skeleton--animated"></div>
          <div class="skeleton-buttons">
            <div class="skeleton-btn skeleton--animated"></div>
            <div class="skeleton-btn skeleton--animated"></div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .skeleton-project-card {
        border: none;
        overflow: hidden;
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

      .skeleton-image {
        width: 100%;
        height: 200px;
        background-color: #e9ecef;
      }

      .skeleton-title {
        height: 24px;
        width: 70%;
        border-radius: 4px;
        margin-bottom: 12px;
        background-color: #e9ecef;
      }

      .skeleton-text {
        height: 14px;
        width: 100%;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-text--short {
        width: 60%;
        margin-bottom: 16px;
      }

      .skeleton-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-bottom: 16px;
      }

      .skeleton-badge {
        height: 22px;
        width: 60px;
        border-radius: 50rem;
        background-color: #e9ecef;
      }

      .skeleton-badge--short {
        width: 45px;
      }

      .skeleton-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: auto;
      }

      .skeleton-link {
        height: 16px;
        width: 100px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton-buttons {
        display: flex;
        gap: 8px;
      }

      .skeleton-btn {
        height: 32px;
        width: 32px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      @media (max-width: 768px) {
        .skeleton-image {
          height: 180px;
        }
      }
    `,
  ],
})
export class SkeletonProjectCardComponent {}
