package com.emmanuelgabe.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a professional, educational, certification, or volunteering experience.
 * Used to build a timeline of career and educational milestones.
 */
@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Company/Organization is required")
    @Size(min = 2, max = 200, message = "Company/Organization must be between 2 and 200 characters")
    @Column(nullable = false, length = 200)
    private String company;

    @NotBlank(message = "Role/Position is required")
    @Size(min = 2, max = 200, message = "Role/Position must be between 2 and 200 characters")
    @Column(nullable = false, length = 200)
    private String role;

    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @PastOrPresent(message = "End date cannot be in the future")
    @Column(name = "end_date")
    private LocalDate endDate;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Experience type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExperienceType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Business logic: Check if the experience is currently ongoing
     * @return true if endDate is null (experience is still in progress)
     */
    public boolean isOngoing() {
        return this.endDate == null;
    }

    /**
     * Business validation: Validate that endDate is after startDate
     * @throws IllegalStateException if endDate is before startDate
     */
    public void validateDates() {
        if (this.endDate != null && this.endDate.isBefore(this.startDate)) {
            throw new IllegalStateException("End date cannot be before start date");
        }
    }
}
