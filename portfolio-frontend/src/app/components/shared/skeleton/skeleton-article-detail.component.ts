import { Component } from '@angular/core';

@Component({
  selector: 'app-skeleton-article-detail',
  standalone: true,
  template: `
    <div
      class="skeleton-article-detail"
      role="status"
      aria-busy="true"
      aria-label="Loading article details"
    >
      <!-- Back button skeleton -->
      <div class="skeleton-back-btn skeleton--animated mb-4"></div>

      <!-- Header skeleton -->
      <header class="mb-4">
        <!-- Title skeleton -->
        <div class="skeleton-title skeleton--animated"></div>
        <div class="skeleton-title skeleton-title--short skeleton--animated"></div>

        <!-- Meta skeleton -->
        <div class="skeleton-meta">
          <div class="skeleton-meta-item skeleton--animated"></div>
          <div class="skeleton-meta-item skeleton--animated"></div>
        </div>

        <!-- Tags skeleton -->
        <div class="skeleton-tags">
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton--animated"></span>
          <span class="skeleton-badge skeleton-badge--short skeleton--animated"></span>
        </div>

        <!-- Excerpt skeleton -->
        <div class="skeleton-excerpt skeleton--animated"></div>
        <div class="skeleton-excerpt skeleton-excerpt--short skeleton--animated"></div>
      </header>

      <hr class="my-4" />

      <!-- Content skeleton -->
      <div class="skeleton-content">
        <!-- Paragraph 1 -->
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton-text--short skeleton--animated"></div>

        <!-- Heading -->
        <div class="skeleton-heading skeleton--animated"></div>

        <!-- Paragraph 2 -->
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton-text--medium skeleton--animated"></div>

        <!-- Code block -->
        <div class="skeleton-code-block skeleton--animated"></div>

        <!-- Paragraph 3 -->
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton--animated"></div>
        <div class="skeleton-text skeleton-text--short skeleton--animated"></div>
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

      .skeleton-back-btn {
        height: 32px;
        width: 100px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton-title {
        height: 48px;
        width: 80%;
        border-radius: 4px;
        margin-bottom: 12px;
        background-color: #e9ecef;
      }

      .skeleton-title--short {
        width: 50%;
        height: 48px;
      }

      .skeleton-meta {
        display: flex;
        gap: 24px;
        margin-bottom: 16px;
      }

      .skeleton-meta-item {
        height: 20px;
        width: 120px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 16px;
      }

      .skeleton-badge {
        height: 24px;
        width: 70px;
        border-radius: 4px;
        background-color: #e9ecef;
      }

      .skeleton-badge--short {
        width: 50px;
      }

      .skeleton-excerpt {
        height: 24px;
        width: 100%;
        border-radius: 4px;
        margin-bottom: 8px;
        background-color: #e9ecef;
      }

      .skeleton-excerpt--short {
        width: 70%;
      }

      .skeleton-content {
        margin-top: 24px;
      }

      .skeleton-text {
        height: 18px;
        width: 100%;
        border-radius: 4px;
        margin-bottom: 12px;
        background-color: #e9ecef;
      }

      .skeleton-text--short {
        width: 60%;
        margin-bottom: 24px;
      }

      .skeleton-text--medium {
        width: 80%;
        margin-bottom: 24px;
      }

      .skeleton-heading {
        height: 32px;
        width: 40%;
        border-radius: 4px;
        margin: 32px 0 16px 0;
        background-color: #e9ecef;
      }

      .skeleton-code-block {
        height: 150px;
        width: 100%;
        border-radius: 8px;
        margin: 24px 0;
        background-color: #e9ecef;
      }

      @media (max-width: 768px) {
        .skeleton-title {
          height: 36px;
          width: 100%;
        }

        .skeleton-title--short {
          width: 70%;
          height: 36px;
        }

        .skeleton-meta {
          flex-direction: column;
          gap: 8px;
        }
      }
    `,
  ],
})
export class SkeletonArticleDetailComponent {}
