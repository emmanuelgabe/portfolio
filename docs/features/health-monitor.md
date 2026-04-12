# Agentic Health Monitor

AI diagnostic agent with tiered autonomy for infrastructure monitoring and remediation.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Diagnostic Tools](#3-diagnostic-tools)
4. [Action Flow](#4-action-flow)
5. [Scheduling](#5-scheduling)
6. [Admin Endpoints](#6-admin-endpoints)
7. [Security](#7-security)
8. [Configuration](#8-configuration)

---

## 1. Overview

The health monitor is an AI agent that observes infrastructure, reasons about anomalies, and proposes corrective actions with human-in-the-loop approval. Actions are classified by risk level with increasing confirmation requirements.

**Key Characteristics**:
- 3-tiered autonomy: GREEN (auto), ORANGE (approval), RED (approval + mandatory reason)
- 7 read-only diagnostic tools (GREEN level)
- 4 recovery actions (ORANGE level) + 1 critical action (RED level)
- Pre-check gate to skip LLM when infrastructure is healthy (cost control)
- Inter-cycle memory: recent reports and rejected actions injected into prompt
- Real-time SSE notifications for new reports and pending actions
- Action expiration: pending actions expire after 1 hour

---

## 2. Architecture

### Backend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `AiHealthAgentService` | `healthmonitor/` | Agentic diagnostic with function calling |
| `NoOpHealthAgentService` | `healthmonitor/` | Fallback when feature disabled |
| `HealthAgentConfig` | `healthmonitor/` | Bean wiring and conditional activation |
| `HealthPromptBuilder` | `healthmonitor/` | System prompt with tool descriptions |
| `HealthPreCheckService` | `healthmonitor/` | Fast non-LLM anomaly gate |
| `HealthMonitorScheduler` | `scheduler/` | Cron-based diagnostic cycles |
| `AdminHealthMonitorController` | `controller/` | Admin REST + SSE endpoints |

### Tool Classes

| Class | Location | Level |
|-------|----------|-------|
| `ServiceHealthTool` | `healthmonitor/tools/` | GREEN |
| `ServiceMetricsTool` | `healthmonitor/tools/` | GREEN |
| `ServiceLogsTool` | `healthmonitor/tools/` | GREEN |
| `RabbitMqQueueTool` | `healthmonitor/tools/` | GREEN |
| `DeadLetterQueueTool` | `healthmonitor/tools/` | GREEN |
| `CircuitBreakerTool` | `healthmonitor/tools/` | GREEN |
| `AuditStatsTool` | `healthmonitor/tools/` | GREEN |

### Frontend Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `HealthMonitorComponent` | `pages/admin/health-monitor/` | Reports timeline, action approval |
| `HealthMonitorService` | `services/` | REST calls + authenticated SSE stream |

### Diagnostic Cycle

```
Scheduler tick (cron)
    |
    v
Pre-check (fast, no LLM)
    |
    +---> Clean --> Persist PRECHECK_ONLY report, skip LLM
    |
    +---> Anomaly detected --> Full LLM analysis
                                    |
                                    v
                              Agent function calling
                              (GREEN tools: observe)
                                    |
                                    v
                              LLM reasoning
                                    |
                                    +---> Report (severity, summary, anomalies)
                                    +---> Proposed actions (ORANGE/RED)
                                              |
                                              v
                                        Persist + SSE notify
                                              |
                                              v
                                        Admin approval/rejection
                                              |
                                              +---> Approved --> Execute
                                              +---> Rejected --> Log reason
                                              +---> Timeout --> Expire
```

---

## 3. Diagnostic Tools

### GREEN Tools (Read-only, automatic)

Used by the agent to gather information. Never persisted as actions.

| Tool | Parameters | Description |
|------|-----------|-------------|
| `checkServiceHealth` | `serviceName` | Container state, health check, restart count |
| `getServiceMetrics` | `serviceName` | CPU%, memory usage vs limit, network I/O |
| `getServiceLogs` | `serviceName, lines` | Last N log lines (max 200, sanitized) |
| `getRabbitMqQueueDepth` | `queueName` | Message backlog and consumer count |
| `checkDeadLetterQueue` | - | DLQ message counts across all queues |
| `checkCircuitBreakers` | - | Resilience4j states and failure rates |
| `getAuditStats` | `days` | Action counts, failures, per-user activity |

**Allowed Services**: spring-backend, angular-frontend, portfolio-db, redis-cache, rabbitmq, kafka, elasticsearch, nginx-portfolio, docker-socket-proxy

**Allowed Queues**: portfolio.email.queue, portfolio.image.queue, portfolio.audit.queue, portfolio.autoreply.queue

### ORANGE Tools (Recovery, requires admin approval)

| Action Type | Description |
|-------------|-------------|
| `FLUSH_REDIS_CACHE` | Flush Redis cache (full or by key pattern) |
| `RETRY_DLQ_MESSAGES` | Retry messages from dead letter queue |
| `REINDEX_ELASTICSEARCH` | Trigger full search reindex batch job |
| `TRIGGER_IMAGE_REPROCESSING` | Trigger image reprocessing batch job |

### RED Tools (Critical, requires approval + mandatory reason)

| Action Type | Description |
|-------------|-------------|
| `RESTART_SERVICE` | Restart a Docker container via socket proxy |

---

## 4. Action Flow

### State Machine

```
PENDING
    +---> [approve] --> APPROVED --> [execute] --> EXECUTED
    |                                          --> FAILED
    +---> [reject]  --> REJECTED
    +---> [timeout] --> EXPIRED
```

### Status Definitions

| Status | Description |
|--------|-------------|
| PENDING | Awaiting admin decision |
| APPROVED | Admin approved, execution triggered |
| REJECTED | Admin rejected with reason |
| EXECUTED | Action executed successfully |
| FAILED | Execution failed |
| EXPIRED | Not approved within expiration window |

### Approval Rules

- **ORANGE actions**: reason optional on approval, mandatory on rejection
- **RED actions**: reason mandatory on both approval and rejection
- **Race condition protection**: atomic compare-and-set queries prevent double-clicks (HTTP 409 on conflict)
- **Optimistic locking**: `@Version` field on entity

### Entities

**HealthReport** (`health_reports` table):

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `cycleType` | VARCHAR(20) | SCHEDULED, ON_DEMAND, PRECHECK_ONLY |
| `severity` | VARCHAR(15) | OK, WARNING, CRITICAL, UNKNOWN |
| `summary` | TEXT | LLM-generated diagnostic summary |
| `anomaliesJson` | TEXT | JSON array of detected anomalies |
| `toolCallsJson` | TEXT | JSON array of tool names invoked |
| `durationMs` | BIGINT | Cycle duration in milliseconds |
| `createdAt` | TIMESTAMP | Immutable timestamp |

**HealthProposedAction** (`health_proposed_actions` table):

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `reportId` | Long | FK to health_reports |
| `level` | VARCHAR(10) | ORANGE or RED |
| `actionType` | VARCHAR(40) | FLUSH_REDIS_CACHE, RESTART_SERVICE, etc. |
| `paramsJson` | TEXT | JSON action parameters |
| `justification` | TEXT | LLM reasoning |
| `status` | VARCHAR(15) | PENDING, APPROVED, REJECTED, EXECUTED, FAILED, EXPIRED |
| `approvedBy` | VARCHAR(100) | Username of approver |
| `approvedAt` | TIMESTAMP | Approval timestamp |
| `executedAt` | TIMESTAMP | Execution timestamp |
| `rejectionReason` | TEXT | Reason for rejection |
| `resultJson` | TEXT | Execution result `{success, message}` |
| `expiresAt` | TIMESTAMP | Action expiration time |
| `createdAt` | TIMESTAMP | Immutable timestamp |
| `version` | BIGINT | Optimistic locking version |

---

## 5. Scheduling

### Cycle Behavior

1. Scheduler fires on cron interval
2. `HealthPreCheckService` runs fast checks (no LLM)
3. If pre-check clean: persist lightweight `PRECHECK_ONLY` report, skip LLM (cost control)
4. If anomaly detected: run full `AiHealthAgentService.analyze()` with function calling
5. Guard against overlapping runs with `AtomicBoolean`
6. Publish SSE events for new reports and pending actions

### On-Demand Analysis

Admin can trigger analysis via `POST /api/admin/health-monitor/analyze-now`. By default this bypasses the pre-check gate (configurable).

### Inter-Cycle Memory

The agent's system prompt includes:
- Recent reports (default: last 3) for trend awareness
- Rejected actions (default: last 5) to avoid re-proposing refused actions

### LLM Resilience

- Wrapped in `@CircuitBreaker` and `@Retry` (Resilience4j)
- Null draft fallback: persist report with severity UNKNOWN
- Total failure fallback: persist UNKNOWN report, log error

---

## 6. Admin Endpoints

**Base Path**: `/api/admin/health-monitor` (requires ADMIN role)

### Reports

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports` | Recent reports (query: `limit`, default 20, max 100) |
| GET | `/reports/{id}` | Specific report by ID |
| POST | `/analyze-now` | Trigger on-demand analysis |

### Actions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actions/pending` | List pending actions |
| GET | `/actions` | All actions (query: `limit`, default 50, max 200) |
| POST | `/actions/{id}/approve` | Approve action (body: `ActionDecisionRequest`) |
| POST | `/actions/{id}/reject` | Reject action (body: `ActionDecisionRequest`, reason mandatory) |

### SSE Stream

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stream` | Authenticated SSE stream |

**SSE Events**: `new_report`, `new_pending_action`, `action_updated`

Frontend uses `fetch()` with Authorization header (not `EventSource`) with auto-reconnect (5 attempts, 5s delay).

---

## 7. Security

### Defense-in-Depth

| Layer | Protection |
|-------|-----------|
| Service whitelist | 9 allowed Docker services, 4 allowed RabbitMQ queues |
| Level mismatch detection | Agent-claimed level vs server-declared level for action type |
| Log sanitization | `<untrusted-logs>` wrapping, pattern filtering for injection attempts |
| Atomic transitions | Compare-and-set queries prevent race conditions |
| Optimistic locking | `@Version` field on proposed actions |
| Docker socket proxy | Restricted API surface (inspect, logs, stats, restart only) |
| ADMIN role | All endpoints protected by Spring Security |

### Docker Socket Proxy

The backend communicates with Docker through a proxy sidecar that exposes only:
- Container inspect
- Container logs
- Container stats
- Container restart

No access to container creation, privileged mode, or host escape.

---

## 8. Configuration

```yaml
ai:
  health-monitor:
    enabled: ${AI_HEALTH_MONITOR_ENABLED:false}
    schedule-cron: ${AI_HEALTH_MONITOR_CRON:0 */15 * * * *}
    on-demand-bypass-precheck: ${AI_HEALTH_MONITOR_BYPASS_PRECHECK:true}
    action-expiration-minutes: ${AI_HEALTH_MONITOR_EXPIRATION_MINUTES:60}
    max-mutations-per-hour: ${AI_HEALTH_MONITOR_MAX_MUTATIONS:10}
    docker:
      host: ${AI_HEALTH_MONITOR_DOCKER_HOST:tcp://docker-socket-proxy:2375}
    history:
      reports-in-context: ${AI_HEALTH_MONITOR_REPORTS_CONTEXT:3}
      rejected-actions-in-context: ${AI_HEALTH_MONITOR_REJECTED_CONTEXT:5}
    logs:
      max-lines-per-call: ${AI_HEALTH_MONITOR_LOG_LINES:50}
```

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | false | Feature master switch |
| `schedule-cron` | Every 15 minutes | Diagnostic cycle interval |
| `on-demand-bypass-precheck` | true | Skip pre-check for manual triggers |
| `action-expiration-minutes` | 60 | Pending action TTL |
| `max-mutations-per-hour` | 10 | Rate limit for executed actions |
| `docker.host` | docker-socket-proxy:2375 | Docker proxy address |
| `history.reports-in-context` | 3 | Recent reports in LLM prompt |
| `history.rejected-actions-in-context` | 5 | Rejected actions in LLM prompt |
| `logs.max-lines-per-call` | 50 | Log tail limit per tool call |

---

## Related Documentation

- [AI Chatbot](./ai-chatbot.md) - Agentic chatbot
- [Contact Moderation](./contact-moderation.md) - AI moderation and auto-reply
- [Circuit Breaker](./circuit-breaker.md) - Resilience4j patterns
- [Batch Processing](./batch-processing.md) - Spring Batch jobs
- [Operations: Observability](../operations/observability.md) - Monitoring stack
