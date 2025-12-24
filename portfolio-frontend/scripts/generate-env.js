/**
 * Generate environment file with feature flags from environment variables.
 *
 * Usage:
 *   SENTRY_DSN_FRONTEND=xxx \
 *   SENTRY_ENABLED=true \
 *   WEB_VITALS_ENABLED=true \
 *   VISITOR_TRACKING_ENABLED=true \
 *   node scripts/generate-env.js
 *
 * Feature flags are disabled by default for GDPR compliance (no cookie consent needed).
 * Enable them only if you implement a cookie consent banner.
 */

const fs = require('fs');
const path = require('path');

const envProdPath = path.join(__dirname, '../src/environments/environment.prod.ts');

// Read current file
let content = fs.readFileSync(envProdPath, 'utf8');
let modified = false;

// Get environment variables
const sentryDsn = process.env.SENTRY_DSN_FRONTEND || '';
const sentryEnabled = process.env.SENTRY_ENABLED === 'true';
const webVitalsEnabled = process.env.WEB_VITALS_ENABLED === 'true';
const visitorTrackingEnabled = process.env.VISITOR_TRACKING_ENABLED === 'true';

// Inject Sentry DSN
if (sentryDsn) {
  content = content.replace(
    /dsn:\s*['"][^'"]*['"]/,
    `dsn: '${sentryDsn}'`
  );
  modified = true;
  console.log('[ENV] Sentry DSN injected');
}

// Inject Sentry enabled flag
if (sentryEnabled) {
  content = content.replace(
    /(sentry:\s*\{[^}]*enabled:\s*)false/,
    '$1true'
  );
  modified = true;
  console.log('[ENV] Sentry enabled');
} else {
  console.log('[ENV] Sentry disabled (default - GDPR compliant)');
}

// Inject Web Vitals enabled flag
if (webVitalsEnabled) {
  content = content.replace(
    /(webVitals:\s*\{[^}]*enabled:\s*)false/,
    '$1true'
  );
  modified = true;
  console.log('[ENV] Web Vitals enabled');
} else {
  console.log('[ENV] Web Vitals disabled (default - GDPR compliant)');
}

// Inject Visitor Tracking enabled flag
if (visitorTrackingEnabled) {
  content = content.replace(
    /(visitorTracking:\s*\{[^}]*enabled:\s*)false/,
    '$1true'
  );
  modified = true;
  console.log('[ENV] Visitor Tracking enabled');
} else {
  console.log('[ENV] Visitor Tracking disabled (default - GDPR compliant)');
}

// Write file if modified
if (modified) {
  fs.writeFileSync(envProdPath, content);
  console.log('[ENV] environment.prod.ts updated');
} else {
  console.log('[ENV] No changes made to environment.prod.ts');
}
