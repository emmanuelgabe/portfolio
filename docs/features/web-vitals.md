# Web Vitals Tracking

Core Web Vitals measurement for frontend performance monitoring.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Metrics Collected](#2-metrics-collected)
3. [Implementation](#3-implementation)
4. [Configuration](#4-configuration)
5. [GDPR Compliance](#5-gdpr-compliance)

---

## 1. Overview

Web Vitals tracking measures Core Web Vitals metrics to monitor frontend performance and user experience.

**Key Capabilities**:
- Largest Contentful Paint (LCP) measurement
- First Input Delay (FID) measurement
- Cumulative Layout Shift (CLS) measurement
- First Contentful Paint (FCP) tracking
- Time to First Byte (TTFB) tracking
- GDPR-compliant feature flag

**Feature Flag**: `WEB_VITALS_ENABLED` controls tracking activation.

---

## 2. Metrics Collected

### 2.1 Core Web Vitals

| Metric | Description | Good | Needs Improvement | Poor |
|--------|-------------|------|-------------------|------|
| **LCP** | Largest Contentful Paint | < 2.5s | 2.5s - 4.0s | > 4.0s |
| **FID** | First Input Delay | < 100ms | 100ms - 300ms | > 300ms |
| **CLS** | Cumulative Layout Shift | < 0.1 | 0.1 - 0.25 | > 0.25 |

### 2.2 Additional Metrics

| Metric | Description | Purpose |
|--------|-------------|---------|
| **FCP** | First Contentful Paint | Time to first visible content |
| **TTFB** | Time to First Byte | Server response time |
| **INP** | Interaction to Next Paint | Responsiveness (replaces FID) |

### 2.3 Metric Data Structure

```typescript
interface WebVitalMetric {
  name: 'LCP' | 'FID' | 'CLS' | 'FCP' | 'TTFB' | 'INP';
  value: number;
  rating: 'good' | 'needs-improvement' | 'poor';
  delta: number;
  id: string;
  navigationType: 'navigate' | 'reload' | 'back-forward';
}
```

---

## 3. Implementation

### 3.1 Service Architecture

**WebVitalsService** (`src/app/services/web-vitals.service.ts`):
- Initializes web-vitals library
- Collects metrics on page load
- Sends data to analytics backend (optional)
- Provides observable for dashboard

### 3.2 Dependencies

```json
{
  "dependencies": {
    "web-vitals": "^3.x"
  }
}
```

### 3.3 Initialization

Service initializes on app bootstrap:
1. Check feature flag (`WEB_VITALS_ENABLED`)
2. Register metric callbacks
3. Collect metrics on page load
4. Report to analytics (if configured)

### 3.4 Metric Collection

```typescript
// Service collects metrics via web-vitals library
// Metrics are collected automatically on:
// - Page load (LCP, FCP, TTFB)
// - User interaction (FID, INP)
// - Page lifecycle (CLS)
```

---

## 4. Configuration

### 4.1 Environment Configuration

```typescript
// environment.ts
export const environment = {
  webVitalsEnabled: true,
  webVitalsEndpoint: '/api/analytics/vitals'
};
```

### 4.2 Feature Flag

**Enable tracking**:
```yaml
WEB_VITALS_ENABLED: true
```

**Disable tracking** (GDPR compliance):
```yaml
WEB_VITALS_ENABLED: false
```

### 4.3 Analytics Integration

When enabled, metrics can be sent to:
- Sentry (performance monitoring)
- Custom analytics endpoint
- Kafka analytics topic
- Console (development only)

---

## 5. GDPR Compliance

### 5.1 Privacy Considerations

Web Vitals tracking is **privacy-friendly** by default:
- No personal data collected
- No cookies required
- No user identification
- Metrics are aggregated

### 5.2 Feature Flag Control

Users can opt-out via feature flag:

```typescript
// Check feature flag before tracking
if (environment.webVitalsEnabled) {
  this.webVitalsService.initialize();
}
```

### 5.3 Data Collected

**Collected**:
- Metric values (numeric)
- Page URL (anonymized)
- Navigation type
- Timestamp

**Not Collected**:
- User IP address
- User ID
- Cookies
- Session data

---

## 6. Usage

### 6.1 Dashboard Integration

Admin dashboard can display:
- Current page LCP/FID/CLS
- Historical trends
- Performance score

### 6.2 Accessing Metrics

```typescript
// Subscribe to metrics stream
this.webVitalsService.metrics$.subscribe(metric => {
  console.log(`${metric.name}: ${metric.value} (${metric.rating})`);
});
```

### 6.3 Manual Reporting

```typescript
// Report to backend
this.webVitalsService.reportToAnalytics(metric);
```

---

## Related Documentation

- [Features: SEO](./seo.md) - SEO optimization
- [Features: PWA](./pwa.md) - Progressive Web App features
- [Architecture: Frontend](../architecture/frontend-architecture.md) - Angular architecture
- [Operations: Observability](../operations/observability.md) - Monitoring overview
