import { Component } from '@angular/core';

@Component({
  selector: 'app-skeleton-project-detail',
  standalone: true,
  template: `
    <div
      class="skeleton-project-detail"
      role="status"
      aria-busy="true"
      aria-label="Loading project details"
    >
      <!-- Header skeleton -->
      <div class="skeleton-header mb-4">
        <div class="skeleton-title skeleton--animated"></div>
        <div class="skeleton-badges">
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton-badge--short skeleton--animated"></span>
        </div>
      </div>

      <!-- Image carousel skeleton -->
      <div class="skeleton-image-carousel skeleton--animated mb-4"></div>

      <!-- Content cards skeleton -->
      <div class="row">
        <!-- Description card -->
        <div class="col-lg-8 mb-4">
          <div class="card h-100 shadow-sm">
            <div class="card-body">
              <div class="skeleton-card-title skeleton--animated"></div>
              <div class="skeleton-text skeleton--animated"></div>
              <div class="skeleton-text skeleton--animated"></div>
              <div class="skeleton-text skeleton--animated"></div>
              <div class="skeleton-text skeleton-text--short skeleton--animated"></div>
            </div>
          </div>
        </div>

        <!-- Tech stack card -->
        <div class="col-lg-4 mb-4">
          <div class="card h-100 shadow-sm">
            <div class="card-body">
              <div class="skeleton-card-title skeleton--animated"></div>
              <div class="skeleton-tech-item skeleton--animated"></div>
              <div class="skeleton-tech-item skeleton--animated"></div>
              <div class="skeleton-tech-item skeleton--animated"></div>
              <div class="skeleton-tech-item skeleton-tech-item--short skeleton--animated"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Links card skeleton -->
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="skeleton-card-title skeleton--animated"></div>
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
        height: 48px;
        width: 60%;
        border-radius: 4px;
        margin-bottom: 16px;
        background-color: #e9ecef;
      }

      .skeleton-badges {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .skeleton-badge {
        height: 28px;
        width: 80px;
        border-radius: 50rem;
        background-color: #e9ecef;
      }

      .skeleton-badge--short {
        width: 60px;
      }

      .skeleton-image-carousel {
        width: 100%;
        height: 400px;
        border-radius: 8px;
        background-color: #e9ecef;
      }

      .skeleton-card-title {
        height: 24px;
        width: 40%;
        border-radius: 4px;
        margin-bottom: 16px;
        background-color: #e9ecef;
      }

      .skeleton-text {
        height: 16px;
        width: 100%;
        border-radius: 4px;
        margin-bottom: 12px;
        background-color: #e9ecef;
      }

      .skeleton-text--short {
        width: 70%;
      }

      .skeleton-tech-item {
        height: 20px;
        width: 80%;
        border-radius: 4px;
        margin-bottom: 12px;
        background-color: #e9ecef;
      }

      .skeleton-tech-item--short {
        width: 60%;
      }

      .skeleton-buttons {
        display: flex;
        gap: 12px;
      }

      .skeleton-btn {
        height: 42px;
        width: 150px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      @media (max-width: 768px) {
        .skeleton-image-carousel {
          height: 250px;
        }

        .skeleton-title {
          width: 80%;
          height: 36px;
        }
      }
    `,
  ],
})
export class SkeletonProjectDetailComponent {}
