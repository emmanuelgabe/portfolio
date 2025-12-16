import { Component } from '@angular/core';

@Component({
  selector: 'app-skeleton-skill-card',
  standalone: true,
  template: `
    <div class="skeleton-skill-card" role="status" aria-busy="true" aria-label="Loading skill">
      <!-- Icon skeleton -->
      <div class="skeleton-icon skeleton--animated"></div>
      <!-- Name skeleton -->
      <div class="skeleton-name skeleton--animated"></div>
    </div>
  `,
  styles: [
    `
      .skeleton-skill-card {
        width: 150px;
        height: 130px;
        padding: 20px;
        border-radius: 18px;
        background: #ffffff;
        box-shadow:
          8px 8px 16px #d0d3d6,
          -8px -8px 16px #ffffff;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
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

      .skeleton-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        background-color: #e9ecef;
      }

      .skeleton-name {
        width: 80px;
        height: 14px;
        border-radius: 4px;
        margin-top: 12px;
        background-color: #e9ecef;
      }

      @media (max-width: 576px) {
        .skeleton-skill-card {
          width: 100%;
          height: 120px;
          padding: 15px;
        }

        .skeleton-icon {
          width: 40px;
          height: 40px;
        }

        .skeleton-name {
          width: 60px;
          height: 12px;
          margin-top: 8px;
        }
      }
    `,
  ],
})
export class SkeletonSkillCardComponent {}
