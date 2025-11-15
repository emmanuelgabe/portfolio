package com.emmanuelgabe.portfolio.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "tags")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    @Column(nullable = false, length = 2000, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Tech stack is required")
    @Size(max = 500, message = "Tech stack cannot exceed 500 characters")
    @Column(name = "tech_stack", nullable = false, length = 500)
    private String techStack;

    @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Size(max = 255, message = "Image URL cannot exceed 255 characters")
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Size(max = 255, message = "Demo URL cannot exceed 255 characters")
    @Column(name = "demo_url", length = 255)
    private String demoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean featured = false;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "project_tags",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // Helper methods for managing bidirectional relationship
    public void addTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        this.tags.add(tag);
        tag.getProjects().add(this);
    }

    public void removeTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        this.tags.remove(tag);
        tag.getProjects().remove(this);
    }

    /**
     * Business logic: Mark project as featured
     * A featured project must have tags and ideally an image and demo URL
     * @throws IllegalStateException if project doesn't meet featured criteria
     */
    public void markAsFeatured() {
        validateForFeatured();
        this.featured = true;
    }

    /**
     * Business logic: Remove featured status from project
     */
    public void unfeature() {
        this.featured = false;
    }

    /**
     * Business validation: Check if project can be featured
     * @throws IllegalStateException if project doesn't meet criteria
     */
    private void validateForFeatured() {
        if (this.tags == null || this.tags.isEmpty()) {
            throw new IllegalStateException("Cannot feature project without tags");
        }
        if (this.imageUrl == null || this.imageUrl.trim().isEmpty()) {
            throw new IllegalStateException("Cannot feature project without an image URL");
        }
    }

    /**
     * Business logic: Check if project can be marked as featured
     * @return true if project meets featured criteria
     */
    public boolean canBeFeatured() {
        return this.tags != null && !this.tags.isEmpty()
                && this.imageUrl != null && !this.imageUrl.trim().isEmpty();
    }
}
