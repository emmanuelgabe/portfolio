# GDPR Compliance

## Overview

This portfolio application is designed to be GDPR-compliant by default. All tracking and analytics features are disabled out of the box, requiring no cookie consent banner.

## Privacy-by-Default Architecture

The application follows a "privacy by default" approach:

- **No tracking cookies** are set by default
- **No analytics** are collected without explicit opt-in
- **No user data** is stored in the database from the contact form
- **Contact form submissions** are sent directly via email and not persisted

## Feature Flags

Three feature flags control optional tracking features. All are disabled (`false`) by default.

### Configuration

**Environment Variables (.env):**
```bash
# Feature flags - disabled by default for GDPR compliance
SENTRY_ENABLED=false
WEB_VITALS_ENABLED=false
VISITOR_TRACKING_ENABLED=false
```

### SENTRY_ENABLED

**Purpose:** Error tracking and performance monitoring

**When enabled:**
- Captures JavaScript errors and exceptions
- Reports to Sentry.io dashboard
- Requires `SENTRY_DSN_FRONTEND` to be configured

**GDPR Impact:**
- Collects IP addresses (anonymized by default)
- Collects browser and device information
- Requires cookie consent if enabled

### WEB_VITALS_ENABLED

**Purpose:** Core Web Vitals performance metrics collection

**When enabled:**
- Collects LCP (Largest Contentful Paint)
- Collects FID (First Input Delay)
- Collects CLS (Cumulative Layout Shift)
- Reports metrics to backend for aggregation

**GDPR Impact:**
- Collects performance data tied to page visits
- Requires cookie consent if enabled

### VISITOR_TRACKING_ENABLED

**Purpose:** Real-time active users count on admin dashboard

**When enabled:**
- Sends heartbeat signals to track active sessions
- Uses sessionStorage for session identification
- Displays live visitor count in admin panel

**GDPR Impact:**
- Creates session identifiers stored in sessionStorage
- Tracks user activity patterns
- Requires cookie consent if enabled

## Enabling Tracking Features

To enable any tracking feature:

1. **Implement a cookie consent banner** (required for GDPR compliance)
2. **Set the environment variable** to `true` in your `.env` file
3. **Rebuild and redeploy** the application

The `generate-env.js` script injects these values during the CI/CD build process.

## Legal Pages

Two legal pages are implemented and required:

### Privacy Policy (`/privacy-policy`)

Content includes:
- Data controller information
- Types of data collected (contact form only)
- Purpose of data processing
- Data retention policy
- User rights (access, rectification, deletion)
- Cookie/tracking disclosure

### Legal Notice (`/legal`)

Content includes:
- Publisher information (name, email, status)
- Hosting details (self-hosted, domain registrar, CDN)
- Intellectual property statement
- Liability disclaimer

## Contact Form Consent

The contact form includes a mandatory consent checkbox:

- Users must accept the privacy policy before submitting
- Form validation prevents submission without consent
- Link to privacy policy opens in new tab

**Implementation:**
- Frontend validation via `Validators.requiredTrue`
- Consent not stored (data not persisted)
- Email sent only after consent acceptance

## Data Flow

```
User submits contact form
         │
         ▼
   Consent checked ────► If not checked: Form blocked
         │
         ▼
   Data sent to backend
         │
         ▼
   Email sent to admin ────► No database storage
         │
         ▼
   Success response
```

## Compliance Checklist

| Requirement | Status |
|-------------|--------|
| Privacy Policy page | Implemented |
| Legal Notice page | Implemented |
| Contact form consent | Implemented |
| No tracking by default | Enabled |
| Feature flags for tracking | Implemented |
| Data not stored in DB | Confirmed |

## Related Files

**Frontend:**
- `src/environments/environment.ts` - Development feature flags
- `src/environments/environment.prod.ts` - Production feature flags
- `src/app/pages/legal/privacy-policy/` - Privacy policy component
- `src/app/pages/legal/legal-notice/` - Legal notice component
- `src/app/components/contact-form/` - Contact form with consent

**Configuration:**
- `.env.example` - Environment variable documentation
- `scripts/generate-env.js` - Feature flag injection script
