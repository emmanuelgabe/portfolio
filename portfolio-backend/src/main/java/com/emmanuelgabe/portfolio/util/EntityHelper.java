package com.emmanuelgabe.portfolio.util;

import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class for common entity lookup and manipulation operations.
 * Reduces boilerplate code in service implementations.
 */
@Slf4j
public final class EntityHelper {

    private EntityHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Find an entity or throw ResourceNotFoundException.
     *
     * @param optional The optional result from repository
     * @param entityName Name of the entity (e.g., "Skill", "Project")
     * @param fieldName Name of the field used for lookup (e.g., "id", "name")
     * @param fieldValue Value of the field
     * @param <T> Entity type
     * @return The entity if found
     * @throws ResourceNotFoundException if entity not found
     */
    public static <T> T findOrThrow(Optional<T> optional, String entityName, String fieldName, Object fieldValue) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(entityName, fieldName, fieldValue));
    }

    /**
     * Ensure slug uniqueness by appending UUID suffix if slug already exists.
     *
     * @param slug The original slug
     * @param existsCheck Predicate to check if slug exists (e.g., repository::existsBySlug)
     * @param logContext Logging context (e.g., "CREATE_ARTICLE", "UPDATE_PROJECT")
     * @return Unique slug (original if unique, or with UUID suffix if collision)
     */
    public static String ensureUniqueSlug(String slug, Predicate<String> existsCheck, String logContext) {
        if (existsCheck.test(slug)) {
            log.warn("[{}] Slug already exists - slug={}", logContext, slug);
            String uniqueSlug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("[{}] Generated unique slug - slug={}", logContext, uniqueSlug);
            return uniqueSlug;
        }
        return slug;
    }

    /**
     * Associate tags with an entity.
     * Fetches tags from repository and applies them using the provided consumer.
     *
     * @param tagIds Collection of tag IDs to associate
     * @param tagRepository Tag repository for fetching tags
     * @param tagConsumer Consumer to apply each tag to the entity (e.g., article::addTag)
     */
    public static void associateTags(Collection<Long> tagIds, TagRepository tagRepository,
                                     Consumer<Tag> tagConsumer) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));
        tags.forEach(tagConsumer);
    }

    /**
     * Update tags for an entity by clearing existing and adding new ones.
     *
     * @param tagIds New collection of tag IDs (null means no change)
     * @param tagRepository Tag repository for fetching tags
     * @param clearAction Action to clear existing tags (e.g., () -> entity.getTags().clear())
     * @param tagConsumer Consumer to apply each tag to the entity
     */
    public static void updateTags(Collection<Long> tagIds, TagRepository tagRepository,
                                  Runnable clearAction, Consumer<Tag> tagConsumer) {
        if (tagIds == null) {
            return;
        }
        clearAction.run();
        if (!tagIds.isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));
            tags.forEach(tagConsumer);
        }
    }

    /**
     * Find a saved image in a collection by matching URL.
     *
     * @param images Collection of images to search
     * @param imageUrl URL to match
     * @param urlExtractor Function to extract URL from image entity
     * @param entityName Name of the entity for error message (e.g., "ArticleImage", "ProjectImage")
     * @param <T> Image entity type
     * @return The matching image entity
     * @throws ResourceNotFoundException if image not found
     */
    public static <T> T findImageByUrl(Collection<T> images, String imageUrl,
                                       java.util.function.Function<T, String> urlExtractor,
                                       String entityName) {
        return images.stream()
                .filter(img -> imageUrl.equals(urlExtractor.apply(img)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(entityName, "imageUrl", imageUrl));
    }
}
