# Experience Management Feature

---

## Table of Contents
1. [Overview](#1-overview)
2. [Experience Types](#2-experience-types)
3. [Experience Structure](#3-experience-structure)
4. [Date Range Management](#4-date-range-management)
5. [Display Order](#5-display-order)
6. [Filtering and Retrieval](#6-filtering-and-retrieval)
7. [Configuration](#7-configuration)

---

## 1. Overview

The Experience Management feature provides a comprehensive system for managing professional, educational, certification, and volunteering experiences. Used to build an interactive timeline showcasing career and educational milestones.

**Key Capabilities**:
- Five distinct experience types for categorization
- Optional fields for flexible experience entries
- Date range tracking with ongoing experience support
- Manual display order with reorder endpoint
- Type-based filtering for targeted retrieval
- Chronological sorting by display order then start date

**Public Access**:
- View all experiences in sorted order
- Filter experiences by type
- View ongoing experiences
- Retrieve recent experiences with configurable limit

**Admin Access**:
- Create, update, and delete experiences
- Reorder experiences manually
- Manage all experience types

---

## 2. Experience Types

### ExperienceType Enum

**Definition** (`com.emmanuelgabe.portfolio.entity.ExperienceType`):

```java
public enum ExperienceType {
    WORK,           // Professional work experience
    STAGE,          // Internship experience
    EDUCATION,      // Educational experience
    CERTIFICATION,  // Professional certifications
    VOLUNTEERING    // Volunteer work and community service
}
```

### Type Descriptions

| Type | Description | Use Cases |
|------|-------------|-----------|
| `WORK` | Professional work experience | Full-time jobs, freelance projects, consulting |
| `STAGE` | Internship experience | Internships, trainee positions, apprenticeships |
| `EDUCATION` | Educational experience | Bachelor's, Master's, PhD degrees, diplomas, online courses |
| `CERTIFICATION` | Professional certifications | Technical certifications (AWS, Azure, Oracle), qualifications |
| `VOLUNTEERING` | Volunteer work | Community service, open source contributions, mentorship |

### Field Naming Conventions

- `WORK`: company = "Acme Corporation", role = "Senior Software Engineer"
- `STAGE`: company = "Tech Startup", role = "Software Development Intern"
- `EDUCATION`: company = "Tech University", role = "Master's Degree in Computer Science"
- `CERTIFICATION`: company = "Oracle", role = "Oracle Certified Professional Java SE 17"
- `VOLUNTEERING`: company = "Local Tech Community", role = "Volunteer Mentor"

---

## 3. Experience Structure

### Entity Model

**Experience Entity** (`com.emmanuelgabe.portfolio.entity.Experience`):

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `id` | Long | No | Auto-generated | Primary key |
| `company` | String | No | max 200 chars | Company or organization name |
| `role` | String | No | max 200 chars | Job title, degree, or certification name |
| `startDate` | LocalDate | No | Past/present | Experience start date |
| `endDate` | LocalDate | No | Past/present, nullable | Experience end date (null = ongoing) |
| `description` | String | Yes | 10-2000 chars | Detailed description |
| `type` | ExperienceType | No | Enum value | Type of experience |
| `showMonths` | boolean | No | Default true | Show months in displayed date range |
| `displayOrder` | Integer | No | Nullable | Manual display order |
| `createdAt` | LocalDateTime | No | Auto-generated | Creation timestamp |
| `updatedAt` | LocalDateTime | No | Auto-updated | Last modification timestamp |

### Computed Properties

**Check if Experience is Ongoing**:
```java
public boolean isOngoing() {
    return this.endDate == null;
}
```

**Date Validation**:
```java
public void validateDates() {
    if (this.endDate != null && this.startDate != null
            && this.endDate.isBefore(this.startDate)) {
        throw new IllegalStateException("End date cannot be before start date");
    }
}
```

---

## 4. Date Range Management

### Date Validation Rules

1. **Start Date**: Optional, must be past or present if provided
2. **End Date**: Optional, must be past or present if provided, must be after start date
3. **Ongoing**: Experience is ongoing when `endDate` is null

### Date Handling in UI

**Display Formats**:
- **Ongoing**: "January 2023 - Present"
- **Completed**: "January 2023 - June 2023"
- **Year Only**: "2022 - 2024" (when `showMonths` is false)
- **Single Day**: "May 10, 2023" (for certifications)

---

## 5. Display Order

### Reorder Functionality

The reorder endpoint allows manual ordering of experiences independent of dates.

**Endpoint**: `POST /api/admin/experiences/reorder`

**Request Body**:
```json
{
  "orderedIds": [3, 1, 2, 5, 4]
}
```

### Sorting Logic

**Repository Methods**:
```java
List<Experience> findAllByOrderByDisplayOrderAscStartDateDesc();
List<Experience> findByTypeOrderByDisplayOrderAscStartDateDesc(ExperienceType type);
List<Experience> findByEndDateIsNullOrderByDisplayOrderAscStartDateDesc();
```

**Sort Priority**:
1. `displayOrder` ascending (experiences with order appear first)
2. `startDate` descending (most recent first)

---

## 6. Filtering and Retrieval

### Filter by Type

**Endpoint**: `GET /api/experiences/type/{type}`

**Supported Types**: WORK, STAGE, EDUCATION, CERTIFICATION, VOLUNTEERING

### Ongoing Experiences

**Endpoint**: `GET /api/experiences/ongoing`

Returns experiences where `endDate` is null.

### Recent Experiences

**Endpoint**: `GET /api/experiences/recent?limit=N`

**Default Limit**: 3 experiences

Returns the N most recent experiences for homepage display.

---

## 7. Configuration

### Entity Configuration

**Table Name**: `experiences`

**Annotations**:
```java
@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Experience { ... }
```

### Validation Annotations

```java
@Size(max = 200)
private String company;  // Optional

@Size(max = 200)
private String role;  // Optional

@PastOrPresent(message = "Start date cannot be in the future")
private LocalDate startDate;  // Optional

@PastOrPresent(message = "End date cannot be in the future")
private LocalDate endDate;  // Optional

@NotBlank(message = "Description is required")
@Size(min = 10, max = 2000)
private String description;  // Required

@Enumerated(EnumType.STRING)
private ExperienceType type;  // Optional

@Column(name = "show_months", nullable = false)
private boolean showMonths = true;

@Min(value = 0)
@Column(name = "display_order")
private Integer displayOrder;  // Optional
```

### MapStruct Configuration

**Mapper Interface**:
```java
@Mapper(componentModel = "spring")
public interface ExperienceMapper {
    ExperienceResponse toResponse(Experience experience);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "showMonths", defaultValue = "true")
    Experience toEntity(CreateExperienceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateExperienceRequest request, @MappingTarget Experience experience);

    @AfterMapping
    default void setOngoingFlag(Experience experience, @MappingTarget ExperienceResponse response) {
        response.setOngoing(experience.getEndDate() == null);
    }
}
```

### Cache Configuration

**Cache Names**:
- `experiences` - All experiences list
- `experiencesByType` - Experiences filtered by type
- `ongoingExperiences` - Ongoing experiences
- `recentExperiences` - Recent experiences

**Cache Eviction**: All caches are evicted on create, update, delete, and reorder operations.

---

## Related Documentation

- [Experiences API](../api/experiences.md) - Complete API reference
- [Architecture: Database Schema](../architecture/database-schema.md) - Database structure
- [Security: RBAC](../security/rbac.md) - Role-based access control
