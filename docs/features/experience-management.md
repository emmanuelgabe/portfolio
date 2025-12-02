# Experience Management Feature

---

## Table of Contents
1. [Overview](#1-overview)
2. [Experience Types](#2-experience-types)
3. [Experience Structure](#3-experience-structure)
4. [Date Range Management](#4-date-range-management)
5. [Ongoing Experiences](#5-ongoing-experiences)
6. [Filtering and Retrieval](#6-filtering-and-retrieval)
7. [Chronological Sorting](#7-chronological-sorting)
8. [Configuration](#8-configuration)

---

## 1. Overview

The Experience Management feature provides a comprehensive system for managing professional, educational, certification, and volunteering experiences. Used to build an interactive timeline showcasing career and educational milestones.

**Key Capabilities**:
- Four distinct experience types for categorization
- Date range tracking with ongoing experience support
- Type-based filtering for targeted retrieval
- Chronological sorting by start date
- Recent experiences summary for homepage
- Full CRUD operations with admin access control
- Date validation and business logic

**Public Access**:
- View all experiences in chronological order
- Filter experiences by type
- View ongoing experiences
- Retrieve recent experiences with configurable limit

**Admin Access**:
- Create, update, and delete experiences
- Manage all experience types
- Edit date ranges and descriptions

---

## 2. Experience Types

### ExperienceType Enum

**Definition** (`com.emmanuelgabe.portfolio.entity.ExperienceType`):

```java
public enum ExperienceType {
    WORK,           // Professional work experience
    EDUCATION,      // Educational experience
    CERTIFICATION,  // Professional certifications
    VOLUNTEERING    // Volunteer work and community service
}
```

### Type Descriptions

| Type | Description | Use Cases |
|------|-------------|-----------|
| `WORK` | Professional work experience | Full-time jobs, internships, freelance projects, consulting |
| `EDUCATION` | Educational experience | Bachelor's, Master's, PhD degrees, diplomas, online courses |
| `CERTIFICATION` | Professional certifications | Technical certifications (AWS, Azure, Oracle), qualifications, licenses |
| `VOLUNTEERING` | Volunteer work | Community service, open source contributions, mentorship, non-profit work |

### Type-Specific Fields

**All Types Share**:
- Company/Organization name
- Role/Position/Degree title
- Start and end dates
- Detailed description
- Created/Updated timestamps

**Field Naming Conventions**:
- `WORK`: company = "Acme Corporation", role = "Senior Software Engineer"
- `EDUCATION`: company = "Tech University", role = "Master's Degree in Computer Science"
- `CERTIFICATION`: company = "Oracle", role = "Oracle Certified Professional Java SE 17"
- `VOLUNTEERING`: company = "Local Tech Community", role = "Volunteer Mentor"

---

## 3. Experience Structure

### Entity Model

**Experience Entity** (`com.emmanuelgabe.portfolio.entity.Experience`):

```java
@Entity
@Table(name = "experiences")
public class Experience {
    private Long id;
    private String company;           // 2-200 characters
    private String role;              // 2-200 characters
    private LocalDate startDate;      // Required, past or present
    private LocalDate endDate;        // Optional, null for ongoing
    private String description;       // 10-2000 characters
    private ExperienceType type;      // Required enum value
    private LocalDateTime createdAt;  // Auto-generated
    private LocalDateTime updatedAt;  // Auto-updated
}
```

### Field Details

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `id` | Long | No | Auto-generated | Primary key |
| `company` | String | Yes | 2-200 chars | Company or organization name |
| `role` | String | Yes | 2-200 chars | Job title, degree, or certification name |
| `startDate` | LocalDate | Yes | Past/present | Experience start date |
| `endDate` | LocalDate | No | Past/present, nullable | Experience end date (null = ongoing) |
| `description` | String | Yes | 10-2000 chars | Detailed description of responsibilities, achievements |
| `type` | ExperienceType | Yes | Enum value | Type of experience |
| `createdAt` | LocalDateTime | No | Auto-generated | Creation timestamp |
| `updatedAt` | LocalDateTime | No | Auto-updated | Last modification timestamp |

### Business Methods

**Check if Experience is Ongoing**:
```java
public boolean isOngoing() {
    return this.endDate == null;
}
```

**Date Validation**:
```java
public void validateDates() {
    if (this.endDate != null && this.endDate.isBefore(this.startDate)) {
        throw new IllegalStateException("End date cannot be before start date");
    }
}
```

---

## 4. Date Range Management

### Date Validation Rules

1. **Start Date**:
   - Must not be null
   - Must be in the past or present (not future)
   - Validated via `@PastOrPresent` annotation

2. **End Date**:
   - Optional (nullable for ongoing experiences)
   - Must be in the past or present if provided
   - Must be equal to or after start date
   - Validated via `@PastOrPresent` annotation

### Date Validation Examples

**Valid Date Ranges**:
```
startDate: 2020-01-15, endDate: 2023-06-30  [OK] Completed experience
startDate: 2023-01-15, endDate: null         [OK] Ongoing experience
startDate: 2023-05-10, endDate: 2023-05-10  [OK] Single-day experience (certifications)
```

**Invalid Date Ranges**:
```
startDate: 2030-01-01, endDate: null         [ERR] Future start date
startDate: 2020-01-15, endDate: 2019-12-31  [ERR] End before start
startDate: 2020-01-15, endDate: 2030-12-31  [ERR] Future end date
```

### Date Handling in UI

**Display Formats**:
- **Ongoing**: "January 2023 - Present"
- **Completed**: "January 2023 - June 2023"
- **Single Day**: "May 10, 2023" (for certifications)

**Duration Calculation**:
```
startDate: 2020-01-15
endDate: 2023-06-30
→ Duration: 3 years 5 months
```

---

## 5. Ongoing Experiences

### Definition

An experience is considered **ongoing** when `endDate` is `null`.

**Endpoint**: `GET /api/experiences/ongoing`

### Use Cases

1. **Current Job**: Display current employment status
2. **Active Studies**: Show ongoing education programs
3. **Long-term Volunteering**: Indicate active volunteer commitments

### Examples

**Ongoing Work Experience**:
```json
{
  "id": 1,
  "company": "Acme Corporation",
  "role": "Senior Software Engineer",
  "startDate": "2023-01-15",
  "endDate": null,
  "type": "WORK",
  "ongoing": true,
  "description": "Leading development of microservices..."
}
```

**Ongoing Volunteering**:
```json
{
  "id": 5,
  "company": "Local Tech Community",
  "role": "Volunteer Mentor",
  "startDate": "2024-06-01",
  "endDate": null,
  "type": "VOLUNTEERING",
  "ongoing": true,
  "description": "Mentoring aspiring developers..."
}
```

### Ongoing Experience Query

**Service Method**:
```java
public List<ExperienceResponse> getOngoingExperiences() {
    return experienceRepository.findByEndDateIsNull()
        .stream()
        .map(experienceMapper::toResponse)
        .collect(Collectors.toList());
}
```

**Repository Method**:
```java
List<Experience> findByEndDateIsNull();
```

---

## 6. Filtering and Retrieval

### Filter by Type

**Endpoint**: `GET /api/experiences/type/{type}`

**Supported Types**:
- `GET /api/experiences/type/WORK` - All work experiences
- `GET /api/experiences/type/EDUCATION` - All education experiences
- `GET /api/experiences/type/CERTIFICATION` - All certifications
- `GET /api/experiences/type/VOLUNTEERING` - All volunteering experiences

**Use Cases**:
- Display work history separately from education
- Show certifications in a dedicated section
- Highlight volunteer work

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/experiences/type/WORK
```

**Example Response**:
```json
[
  {
    "id": 1,
    "company": "Acme Corporation",
    "role": "Senior Software Engineer",
    "startDate": "2023-01-15",
    "endDate": null,
    "type": "WORK",
    "ongoing": true
  },
  {
    "id": 4,
    "company": "Previous Company",
    "role": "Software Developer",
    "startDate": "2020-06-01",
    "endDate": "2022-12-31",
    "type": "WORK",
    "ongoing": false
  }
]
```

### Recent Experiences

**Endpoint**: `GET /api/experiences/recent?limit=N`

**Default Limit**: 3 experiences

**Use Case**: Homepage summary showing most recent achievements

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | 3 | Maximum number of experiences to return |

**Example Request**:
```bash
# Get 5 most recent experiences
curl -X GET "http://localhost:8080/api/experiences/recent?limit=5"
```

**Sorting**: Ordered by `startDate` descending (most recent first)

---

## 7. Chronological Sorting

### Default Sort Order

**All Listing Endpoints**: Sorted by `startDate` in **descending order** (newest first)

**Rationale**: Users typically want to see the most recent experiences first

### Sort Implementation

**Service Layer**:
```java
public List<ExperienceResponse> getAllExperiences() {
    return experienceRepository.findAllByOrderByStartDateDesc()
        .stream()
        .map(experienceMapper::toResponse)
        .collect(Collectors.toList());
}
```

**Repository Method**:
```java
List<Experience> findAllByOrderByStartDateDesc();
List<Experience> findByTypeOrderByStartDateDesc(ExperienceType type);
```

### Timeline Display

**Chronological Order**:
1. Current ongoing experiences (most recent start date first)
2. Completed experiences (most recent start date first)
3. Older experiences

**Example Timeline**:
```
2024-06-01 - Present    : Volunteer Mentor (VOLUNTEERING)
2023-01-15 - Present    : Senior Software Engineer (WORK)
2023-05-10 - 2023-05-10 : Oracle Java Certification (CERTIFICATION)
2020-09-01 - 2022-06-30 : Master's Degree (EDUCATION)
2018-06-01 - 2020-05-31 : Software Developer (WORK)
```

---

## 8. Configuration

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

**Field Validation**:
```java
@NotBlank(message = "Company/Organization is required")
@Size(min = 2, max = 200)
private String company;

@NotNull(message = "Start date is required")
@PastOrPresent(message = "Start date cannot be in the future")
private LocalDate startDate;

@PastOrPresent(message = "End date cannot be in the future")
private LocalDate endDate;  // Nullable

@NotBlank(message = "Description is required")
@Size(min = 10, max = 2000)
private String description;

@NotNull(message = "Experience type is required")
@Enumerated(EnumType.STRING)
private ExperienceType type;
```

### MapStruct Configuration

**Mapper Interface**:
```java
@Mapper(componentModel = "spring")
public interface ExperienceMapper {
    ExperienceResponse toResponse(Experience experience);

    @Mapping(target = "id", ignore = true)
    Experience toEntity(CreateExperienceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateExperienceRequest request, @MappingTarget Experience experience);
}
```

**Partial Update Support**: `UpdateExperienceRequest` allows null values, only provided fields are updated

---

## Related Documentation

- [Experiences API](../api/experiences.md) - Complete API reference
- [Architecture: Database Schema](../architecture/database-schema.md) - Database structure
- [Security: RBAC](../security/rbac.md) - Role-based access control
