# Application Caching

---

## Table of Contents
1. [Overview](#1-overview)
2. [Cache Configuration](#2-cache-configuration)
3. [Cached Services](#3-cached-services)
4. [Cache Keys](#4-cache-keys)
5. [Actuator Endpoints](#5-actuator-endpoints)
6. [Configuration](#6-configuration)

---

## 1. Overview

Redis-based application caching for frequently accessed, rarely modified data. Reduces database load and improves response times for public endpoints.

**Cached Resources**:
- Site configuration (singleton)
- Skills list and categories
- Projects list and featured projects
- Experiences by type
- Tags

**Strategy**: Cache-aside pattern with Spring Cache annotations

**Invalidation**: Automatic eviction on create/update/delete operations

**Storage**: Redis with JSON serialization

---

## 2. Cache Configuration

### Cache Manager

**Bean**: `RedisCacheManager` in `CacheConfig.java`

**Features**:
- JSON serialization (Jackson `GenericJackson2JsonRedisSerializer`)
- Key prefix: `portfolio:`
- Statistics enabled for Actuator metrics
- Per-cache TTL configuration

### TTL Configuration

| Cache Name | TTL | Use Case |
|------------|-----|----------|
| `siteConfig` | 6 hours | Singleton entity, rarely changes |
| `skills` | 4 hours | Skills list, infrequent updates |
| `projects` | 2 hours | Project list, moderate updates |
| `experiences` | 4 hours | Timeline data, infrequent updates |
| `tags` | 4 hours | Small reference table |

### Serialization

**Key Serializer**: `StringRedisSerializer`

**Value Serializer**: `GenericJackson2JsonRedisSerializer`

**Benefits**:
- Human-readable values in Redis CLI
- Compatible with existing DTOs (no `Serializable` required)
- Type information preserved for deserialization

---

## 3. Cached Services

### SiteConfigurationService

| Method | Annotation | Key |
|--------|------------|-----|
| `getSiteConfiguration()` | `@Cacheable` | `'current'` |
| `updateSiteConfiguration()` | `@CacheEvict(allEntries=true)` | - |
| `uploadProfileImage()` | `@CacheEvict(allEntries=true)` | - |
| `deleteProfileImage()` | `@CacheEvict(allEntries=true)` | - |

### SkillService

| Method | Annotation | Key |
|--------|------------|-----|
| `getAllSkills()` | `@Cacheable` | `'all'` |
| `getSkillById(id)` | `@Cacheable` | `#id` |
| `getSkillsByCategory(category)` | `@Cacheable` | `'category:' + #category.name()` |
| `createSkill()` | `@CacheEvict(allEntries=true)` | - |
| `updateSkill()` | `@CacheEvict(allEntries=true)` | - |
| `deleteSkill()` | `@CacheEvict(allEntries=true)` | - |
| `uploadSkillIcon()` | `@CacheEvict(allEntries=true)` | - |

### ProjectService

| Method | Annotation | Key |
|--------|------------|-----|
| `getAllProjects()` | `@Cacheable` | `'all'` |
| `getFeaturedProjects()` | `@Cacheable` | `'featured'` |
| `getProjectById(id)` | `@Cacheable` | `#id` |
| `createProject()` | `@CacheEvict(allEntries=true)` | - |
| `updateProject()` | `@CacheEvict(allEntries=true)` | - |
| `deleteProject()` | `@CacheEvict(allEntries=true)` | - |
| Image operations | `@CacheEvict(allEntries=true)` | - |

### ExperienceService

| Method | Annotation | Key |
|--------|------------|-----|
| `getAllExperiences()` | `@Cacheable` | `'all'` |
| `getExperienceById(id)` | `@Cacheable` | `#id` |
| `getExperiencesByType(type)` | `@Cacheable` | `'type:' + #type.name()` |
| `getOngoingExperiences()` | `@Cacheable` | `'ongoing'` |
| `getRecentExperiences(limit)` | `@Cacheable` | `'recent:' + #limit` |
| `createExperience()` | `@CacheEvict(allEntries=true)` | - |
| `updateExperience()` | `@CacheEvict(allEntries=true)` | - |
| `deleteExperience()` | `@CacheEvict(allEntries=true)` | - |

### TagService

| Method | Annotation | Key |
|--------|------------|-----|
| `getAllTags()` | `@Cacheable` | `'all'` |
| `getTagById(id)` | `@Cacheable` | `#id` |
| `getTagByName(name)` | `@Cacheable` | `'name:' + #name` |
| `createTag()` | `@CacheEvict(allEntries=true)` | - |
| `updateTag()` | `@CacheEvict(allEntries=true)` | - |
| `deleteTag()` | `@CacheEvict(allEntries=true)` | - |

---

## 4. Cache Keys

### Key Format

**Pattern**: `portfolio:{cacheName}::{key}`

### Examples

```
portfolio:siteConfig::current
portfolio:skills::all
portfolio:skills::1
portfolio:skills::category:FRONTEND
portfolio:projects::all
portfolio:projects::featured
portfolio:projects::5
portfolio:experiences::all
portfolio:experiences::type:WORK
portfolio:experiences::ongoing
portfolio:experiences::recent:5
portfolio:tags::all
portfolio:tags::name:Angular
```

### Redis CLI Commands

**View all cache keys**:
```bash
redis-cli KEYS "portfolio:*"
```

**View specific cache value**:
```bash
redis-cli GET "portfolio:skills::all"
```

**Check TTL**:
```bash
redis-cli TTL "portfolio:siteConfig::current"
```

**Clear specific cache**:
```bash
redis-cli DEL "portfolio:skills::all"
```

**Clear all caches**:
```bash
redis-cli KEYS "portfolio:*" | xargs redis-cli DEL
```

---

## 5. Actuator Endpoints

### Cache Management

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/actuator/caches` | GET | List all caches and their configuration |
| `/actuator/caches/{name}` | GET | Details of a specific cache |
| `/actuator/caches` | DELETE | Clear all caches |
| `/actuator/caches/{name}` | DELETE | Clear a specific cache |

### Metrics (Prometheus)

**Available metrics**:

| Metric | Description | Tags |
|--------|-------------|------|
| `cache_gets_total` | Total cache requests | `cache`, `result` (hit/miss) |
| `cache_puts_total` | Total cache writes | `cache` |
| `cache_evictions_total` | Total evictions | `cache` |

**Example Prometheus queries**:

```promql
# Cache hit ratio for skills cache
sum(rate(cache_gets_total{cache="skills",result="hit"}[5m])) /
sum(rate(cache_gets_total{cache="skills"}[5m]))

# Total cache misses across all caches
sum(rate(cache_gets_total{result="miss"}[5m]))
```

---

## 6. Configuration

### Application Properties

**application.yml**:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # Default 1 hour (ms)

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,caches
```

### CacheConfig Bean

**Location**: `com.emmanuelgabe.portfolio.config.CacheConfig`

```java
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON serializer for cache values
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();

        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(...)
            .serializeValuesWith(...)
            .prefixCacheNameWith("portfolio:");

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "siteConfig", defaultConfig.entryTtl(Duration.ofHours(6)),
            "skills", defaultConfig.entryTtl(Duration.ofHours(4)),
            "projects", defaultConfig.entryTtl(Duration.ofHours(2)),
            "experiences", defaultConfig.entryTtl(Duration.ofHours(4)),
            "tags", defaultConfig.entryTtl(Duration.ofHours(4))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .enableStatistics()
            .build();
    }
}
```

### Dependencies

**build.gradle**:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
}
```

---

## Related Documentation

- [Security: Rate Limiting](../security/rate-limiting.md) - Redis-based rate limiting (same infrastructure)
- [Reference: Configuration Properties](../reference/configuration-properties.md) - All configuration options
- [Operations: Health Checks](../operations/health-checks.md) - Actuator endpoints