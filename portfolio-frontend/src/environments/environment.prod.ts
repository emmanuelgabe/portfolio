import { VERSION } from './version';

export const environment = {
  production: true,
  apiUrl: 'https://emmanuelgabe.com',
  baseUrl: 'https://emmanuelgabe.com',
  version: VERSION,
  logLevel: 'WARN',
  // Feature flags - Privacy by default (GDPR compliant)
  // Disabled by default to avoid cookie consent requirement
  // Enable via environment variables during CI/CD build if needed
  sentry: {
    // DSN injected during CI/CD build via SENTRY_DSN_FRONTEND secret
    dsn: '',
    enabled: false, // Set to true via SENTRY_ENABLED env var
    tracesSampleRate: 0.05,
  },
  webVitals: {
    enabled: false, // Set to true via WEB_VITALS_ENABLED env var
  },
  visitorTracking: {
    enabled: false, // Set to true via VISITOR_TRACKING_ENABLED env var
  },
};
