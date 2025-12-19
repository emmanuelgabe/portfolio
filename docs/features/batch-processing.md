# Batch Processing

Spring Batch jobs for scheduled maintenance, reporting, and data processing tasks.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Scheduled Jobs](#scheduled-jobs)
4. [Manual Jobs](#manual-jobs)
5. [Configuration](#configuration)
6. [Database Schema](#database-schema)
7. [Related Documentation](#related-documentation)

---

## Overview

The batch processing system provides scheduled and on-demand jobs:

- **Audit cleanup** - Remove old audit logs (retention policy)
- **Audit reports** - Generate monthly PDF reports
- **Stats aggregation** - Daily statistics collection
- **Sitemap generation** - Dynamic sitemap.xml updates
- **Search reindex** - Weekly Elasticsearch synchronization
- **Image reprocessing** - On-demand image quality updates

Key characteristics:
- Conditional activation via `batch.enabled` property
- Configurable cron expressions per job
- Job execution tracking via Spring Batch metadata
- Fault-tolerant processing with skip limits
- Admin dashboard integration for monitoring

---

## Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `BatchJobScheduler` | `batch/` | Scheduled job launcher |
| `AuditCleanupJobConfig` | `batch/` | Audit retention job |
| `AuditReportJobConfig` | `batch/` | Monthly report generation |
| `StatsAggregationJobConfig` | `batch/` | Daily statistics job |
| `SitemapGenerationJobConfig` | `batch/` | Sitemap regeneration job |
| `SearchReindexJobConfig` | `batch/` | Elasticsearch reindex job |
| `ImageReprocessingJobConfig` | `batch/` | Image reprocessing job |
| `AdminBatchController` | `controller/` | REST API for manual triggers |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `BatchService` | `services/` | HTTP service for batch API |
| `DashboardComponent` | `pages/admin/dashboard/` | Batch job status display |

### Job Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    BatchJobScheduler                         │
│  @Scheduled(cron) triggers at configured intervals          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      JobLauncher                             │
│  Creates JobExecution with timestamp parameter              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Job (Tasklet or Chunk)                    │
│  Executes business logic, logs results                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    JobRepository                             │
│  Persists execution metadata (status, exit code, counts)    │
└─────────────────────────────────────────────────────────────┘
```

---

## Scheduled Jobs

### Audit Cleanup Job

Deletes audit logs older than the configured retention period.

| Property | Value |
|----------|-------|
| Job Name | `auditCleanupJob` |
| Schedule | Daily at 2:00 AM |
| Cron | `0 0 2 * * ?` |
| Retention | 90 days (configurable) |

**Tasklet Logic:**
```java
LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
long deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
```

---

### Audit Report Job

Generates monthly PDF reports with audit statistics.

| Property | Value |
|----------|-------|
| Job Name | `auditReportJob` |
| Schedule | 1st of month at 3:00 AM |
| Cron | `0 0 3 1 * ?` |
| Output | PDF file in reports directory |

**Report Contents:**
- Total actions by type
- Actions by entity type
- Actions by user
- Daily activity summary
- Top modified entities

---

### Stats Aggregation Job

Collects and stores daily portfolio statistics.

| Property | Value |
|----------|-------|
| Job Name | `statsAggregationJob` |
| Schedule | Daily at midnight |
| Cron | `0 0 0 * * ?` |
| Target | `daily_stats` table |

**Aggregated Metrics:**

| Metric | Source |
|--------|--------|
| `total_projects` | ProjectRepository.count() |
| `total_articles` | ArticleRepository.count() |
| `total_skills` | SkillRepository.count() |
| `total_experiences` | ExperienceRepository.count() |
| `total_tags` | TagRepository.count() |
| `published_articles` | Query published articles |
| `draft_articles` | Calculated from total |
| `total_project_images` | ProjectImageRepository.count() |
| `total_article_images` | ArticleImageRepository.count() |
| `unique_visitors` | Redis daily visitor set |
| `audit_events_count` | AuditLogRepository count |
| `failed_audit_events` | Failed audit events count |

**Idempotency:** Skips execution if stats already exist for the date.

---

### Sitemap Generation Job

Regenerates the sitemap.xml file with current content.

| Property | Value |
|----------|-------|
| Job Name | `sitemapGenerationJob` |
| Schedule | Daily at 4:00 AM |
| Cron | `0 0 4 * * ?` |
| Output | sitemap.xml in public directory |

**Sitemap Entries:**
- Static pages (home, contact, blog)
- Published articles with lastmod dates
- Published projects with lastmod dates

---

### Search Reindex Job

Rebuilds all Elasticsearch indices from database.

| Property | Value |
|----------|-------|
| Job Name | `searchReindexJob` |
| Schedule | Sundays at 3:00 AM |
| Cron | `0 0 3 * * SUN` |
| Indices | articles, projects, experiences |

**Reindex Process:**
1. Clear existing index documents
2. Fetch all entities from database
3. Index each entity type
4. Return total indexed count

---

## Manual Jobs

### Image Reprocessing Job

On-demand job to reprocess images with updated quality settings.

| Property | Value |
|----------|-------|
| Job Name | `imageReprocessingJob` |
| Trigger | Manual via REST API |
| Chunk Size | 10 (configurable) |
| Skip Limit | 100 errors |

**Steps:**

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| `reprocessProjectImagesStep` | READY project images | Validate original exists | Reprocess to WebP |
| `reprocessArticleImagesStep` | READY article images | Validate original exists | Reprocess to WebP |

**REST Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/batch/image-reprocessing/stats` | Get eligible images count |
| GET | `/api/admin/batch/image-reprocessing/last` | Get last job execution |
| POST | `/api/admin/batch/image-reprocessing/run` | Trigger reprocessing |

**Response Models:**

```typescript
interface ImageReprocessingStats {
  projectImagesEligible: number;
  articleImagesEligible: number;
  totalEligible: number;
  timestamp: string;
}

interface LastJobInfo {
  lastJobId: number | null;
  lastJobStatus: string | null;
  lastJobDate: string | null;
  exitCode: string | null;
  processedCount: number;
  errorCount: number;
}

interface JobRunResult {
  jobId: number;
  status: string;
  startTime: string;
  exitCode: string;
}
```

---

## Configuration

### Application Properties

```yaml
# Enable batch processing
batch:
  enabled: ${BATCH_ENABLED:false}

  # Audit cleanup job
  audit-cleanup:
    cron: "0 0 2 * * ?"        # Daily at 2:00 AM
    retention-days: 90          # Keep logs for 90 days

  # Audit report job
  audit-report:
    cron: "0 0 3 1 * ?"        # 1st of month at 3:00 AM

  # Stats aggregation job
  stats-aggregation:
    cron: "0 0 0 * * ?"        # Daily at midnight

  # Sitemap generation job
  sitemap:
    cron: "0 0 4 * * ?"        # Daily at 4:00 AM

  # Search reindex job
  search-reindex:
    cron: "0 0 3 * * SUN"      # Sundays at 3:00 AM

  # Image reprocessing job
  image-reprocessing:
    chunk-size: 10              # Images per chunk
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BATCH_ENABLED` | `false` | Enable batch job execution |
| `AUDIT_RETENTION_DAYS` | `90` | Days to retain audit logs |

---

## Database Schema

### Spring Batch Metadata Tables

Spring Batch creates its metadata tables automatically:

| Table | Purpose |
|-------|---------|
| `BATCH_JOB_INSTANCE` | Job instances |
| `BATCH_JOB_EXECUTION` | Job executions |
| `BATCH_JOB_EXECUTION_PARAMS` | Job parameters |
| `BATCH_STEP_EXECUTION` | Step executions |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step context |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job context |

### Daily Stats Table

```sql
CREATE TABLE daily_stats (
    id BIGSERIAL PRIMARY KEY,
    stats_date DATE NOT NULL UNIQUE,
    total_projects BIGINT DEFAULT 0,
    total_articles BIGINT DEFAULT 0,
    published_articles BIGINT DEFAULT 0,
    draft_articles BIGINT DEFAULT 0,
    total_skills BIGINT DEFAULT 0,
    total_experiences BIGINT DEFAULT 0,
    total_tags BIGINT DEFAULT 0,
    total_project_images BIGINT DEFAULT 0,
    total_article_images BIGINT DEFAULT 0,
    unique_visitors BIGINT DEFAULT 0,
    audit_events_count BIGINT DEFAULT 0,
    failed_audit_events BIGINT DEFAULT 0,
    contact_submissions BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_stats_date ON daily_stats(stats_date DESC);
```

---

## Related Documentation

- [Image Processing](./image-processing.md) - Image optimization pipeline
- [Audit System](./audit-system.md) - Audit logging feature
- [Search](./search.md) - Elasticsearch search feature
- [Sitemap](./sitemap.md) - Sitemap generation

