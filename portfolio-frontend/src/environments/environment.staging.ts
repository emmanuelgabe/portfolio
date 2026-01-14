import { VERSION } from './version';

export const environment = {
  production: false,
  apiUrl: 'https://staging.emmanuelgabe.com',
  baseUrl: 'https://staging.emmanuelgabe.com',
  version: VERSION,
  logLevel: 'INFO',
  sentry: {
    dsn: '',
    enabled: false,
    tracesSampleRate: 0.1,
  },
  webVitals: {
    enabled: false,
  },
  visitorTracking: {
    enabled: false,
  },
};
