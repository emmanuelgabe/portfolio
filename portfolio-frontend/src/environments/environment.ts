import { VERSION } from './version';

export const environment = {
  production: false,
  apiUrl: '',
  baseUrl: 'http://localhost:4200',
  version: VERSION,
  logLevel: 'DEBUG',
  // Feature flags - Privacy by default (GDPR compliant)
  // These features are disabled by default to avoid cookie consent requirement
  sentry: {
    dsn: '',
    enabled: false, // Set to true via SENTRY_ENABLED env var if needed
    tracesSampleRate: 0.1,
  },
  webVitals: {
    enabled: false, // Set to true via WEB_VITALS_ENABLED env var if needed
  },
  visitorTracking: {
    enabled: false, // Set to true via VISITOR_TRACKING_ENABLED env var if needed
  },
};
