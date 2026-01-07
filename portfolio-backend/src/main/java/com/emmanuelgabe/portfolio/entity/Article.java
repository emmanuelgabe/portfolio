package com.emmanuelgabe.portfolio.entity;

import com.emmanuelgabe.portfolio.entity.listener.ArticleEntityListener;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a blog article with Markdown content.
 * Supports draft/publish workflow, tags, and embedded images.
 */
@Entity
@Table(name = "articles")
@EntityListeners(ArticleEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(min = 3, max = 200, message = "Slug must be between 3 and 200 characters")
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @NotBlank(message = "Content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    @Column(length = 500)
    private String excerpt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private boolean draft = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    @Min(value = 0, message = "Display order must be at least 0")
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ArticleImage> images = new HashSet<>();

    /**
     * Publishes the article by setting draft to false and recording the publication timestamp.
     */
    public void publish() {
        this.draft = false;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    /**
     * Unpublishes the article by setting draft to true.
     */
    public void unpublish() {
        this.draft = true;
    }

    /**
     * Checks if the article is currently published and visible to the public.
     *
     * @return true if the article is not a draft and has a valid publication date
     */
    public boolean isPublished() {
        return !this.draft
            && this.publishedAt != null
            && !this.publishedAt.isAfter(LocalDateTime.now());
    }

    /**
     * Generates a URL-friendly slug from the title if no slug is set.
     * Converts to lowercase, removes special characters, and replaces spaces with hyphens.
     */
    public void generateSlug() {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = this.title
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        }
    }

    /**
     * Calculates the estimated reading time based on word count.
     * Assumes average reading speed of 200 words per minute.
     */
    public void calculateReadingTime() {
        if (this.content != null && !this.content.isBlank()) {
            int wordCount = this.content.split("\\s+").length;
            this.readingTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / 200.0));
        }
    }

    /**
     * Helper method to add a tag to the article (maintains bidirectional relationship).
     *
     * @param tag the tag to add
     */
    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    /**
     * Helper method to remove a tag from the article (maintains bidirectional relationship).
     *
     * @param tag the tag to remove
     */
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    /**
     * Helper method to add an image to the article (maintains bidirectional relationship).
     *
     * @param image the image to add
     */
    public void addImage(ArticleImage image) {
        this.images.add(image);
        image.setArticle(this);
    }

    /**
     * Helper method to remove an image from the article (maintains bidirectional relationship).
     *
     * @param image the image to remove
     */
    public void removeImage(ArticleImage image) {
        this.images.remove(image);
        image.setArticle(null);
    }
}
