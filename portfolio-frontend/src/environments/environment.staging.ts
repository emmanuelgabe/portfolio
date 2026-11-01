import { VERSION } from './version';

export const environment = {
  production: false,
  // Same origin: the staging nginx proxies /api to the staging backend
  apiUrl: '',
  baseUrl: 'https://staging.emmanuelgabe.com',
  version: VERSION,
  // Feature flags - Privacy by default (GDPR compliant)
  sentry: {
    dsn: '',
    enabled: false, // Set to true via SENTRY_ENABLED env var
    tracesSampleRate: 0.1,
  },
  webVitals: {
    enabled: false,
  },
  visitorTracking: {
    enabled: false,
  },
};
