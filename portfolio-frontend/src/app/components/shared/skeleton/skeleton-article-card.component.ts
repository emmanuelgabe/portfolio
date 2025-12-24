import { Component } from '@angular/core';

@Component({
  selector: 'app-skeleton-article-card',
  standalone: true,
  template: `
    <div
      class="card h-100 shadow-sm skeleton-article-card"
      role="status"
      aria-busy="true"
      aria-label="Loading article"
    >
      <div class="card-body d-flex flex-column">
        <!-- Title skeleton -->
        <div class="skeleton-title skeleton--animated"></div>

        <!-- Excerpt skeleton (3 lines) -->
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton-text--short skeleton--animated"></div>

        <!-- Tags skeleton -->
        <div class="skeleton-tags">
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton-badge--short skeleton--animated"></span>
        </div>

        <!-- Author skeleton -->
        <div class="skeleton-meta skeleton--animated"></div>

        <!-- Date skeleton -->
        <div class="skeleton-date skeleton--animated"></div>

        <!-- Button skeleton -->
        <div class="skeleton-button skeleton--animated"></div>
      </div>
    </div>
  `,
  styles: [
    `
      .skeleton-article-card {
        border: 1px solid #dee2e6;
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

      .skeleton-title {
        height: 24px;
        width: 80%;
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
        width: 70%;
        margin-bottom: 16px;
      }

      .skeleton-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-bottom: 12px;
      }

      .skeleton-badge {
        height: 22px;
        width: 55px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton-badge--short {
        width: 40px;
      }

      .skeleton-meta {
        height: 14px;
        width: 120px;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-date {
        height: 14px;
        width: 100px;
        border-radius: 4px;
        margin-bottom: 16px;
        background-color: #e9ecef;
      }

      .skeleton-button {
        height: 32px;
        width: 120px;
        border-radius: 4px;
        margin-top: auto;
        background-color: #e9ecef;
      }
    `,
  ],
})
export class SkeletonArticleCardComponent {}
