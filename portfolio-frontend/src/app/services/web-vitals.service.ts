import { Injectable, inject } from '@angular/core';
import { onCLS, onINP, onLCP, onFCP, onTTFB, Metric } from 'web-vitals';
import * as Sentry from '@sentry/angular';
import { environment } from '../../environments/environment';
import { LoggerService } from './logger.service';

/**
 * Service for collecting and reporting Web Vitals metrics.
 * Measures Core Web Vitals (LCP, INP, CLS) and additional metrics (FCP, TTFB).
 */
@Injectable({
  providedIn: 'root',
})
export class WebVitalsService {
  private readonly logger = inject(LoggerService);

  /**
   * Initialize Web Vitals collection.
   * Should be called once during application startup.
   */
  init(): void {
    if (!environment.webVitals.enabled) {
      return;
    }

    this.logger.info('[WEB_VITALS] Initializing Web Vitals collection');

    onCLS(this.handleMetric.bind(this));
    onINP(this.handleMetric.bind(this));
    onLCP(this.handleMetric.bind(this));
    onFCP(this.handleMetric.bind(this));
    onTTFB(this.handleMetric.bind(this));
  }

  private handleMetric(metric: Metric): void {
    this.logger.debug('[WEB_VITALS] Metric recorded', {
      name: metric.name,
      value: metric.value,
      rating: metric.rating,
      id: metric.id,
    });

    this.reportToSentry(metric);
  }

  private reportToSentry(metric: Metric): void {
    if (!environment.sentry.enabled) {
      return;
    }

    const unit = this.getMetricUnit(metric.name);
    Sentry.setMeasurement(metric.name, metric.value, unit);

    if (metric.rating === 'poor') {
      Sentry.addBreadcrumb({
        category: 'web-vitals',
        message: `Poor ${metric.name} score detected`,
        level: 'warning',
        data: {
          name: metric.name,
          value: metric.value,
          rating: metric.rating,
        },
      });
    }
  }

  private getMetricUnit(metricName: string): 'millisecond' | 'none' {
    switch (metricName) {
      case 'CLS':
        return 'none';
      default:
        return 'millisecond';
    }
  }
}
