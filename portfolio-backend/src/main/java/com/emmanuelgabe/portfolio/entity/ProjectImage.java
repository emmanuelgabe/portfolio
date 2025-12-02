package com.emmanuelgabe.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an image uploaded and associated with a project.
 * Supports multiple images per project with ordering and primary designation.
 */
@Entity
@Table(name = "project_images")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank(message = "Image URL is required")
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Size(max = 255, message = "Thumbnail URL must not exceed 255 characters")
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Column(name = "alt_text")
    private String altText;

    @Size(max = 500, message = "Caption must not exceed 500 characters")
    @Column(name = "caption")
    private String caption;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public ProjectImage(Project project, String imageUrl, String thumbnailUrl) {
        this.project = project;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public ProjectImage(Project project, String imageUrl, String thumbnailUrl, String altText, String caption) {
        this.project = project;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.altText = altText;
        this.caption = caption;
    }
}
