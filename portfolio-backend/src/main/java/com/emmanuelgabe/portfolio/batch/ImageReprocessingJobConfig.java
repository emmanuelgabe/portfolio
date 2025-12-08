package com.emmanuelgabe.portfolio.batch;

import com.emmanuelgabe.portfolio.entity.ArticleImage;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.entity.ProjectImage;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.service.ImageProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Batch job configuration for reprocessing images.
 * Reprocesses all READY images from their original files when quality settings change.
 *
 * <p>This job demonstrates the complete Spring Batch pattern:
 * <ul>
 *   <li>ItemReader: Reads images eligible for reprocessing</li>
 *   <li>ItemProcessor: Validates original file exists</li>
 *   <li>ItemWriter: Performs actual reprocessing</li>
 * </ul>
 *
 * <p>Job can be triggered manually via REST endpoint or scheduled.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class ImageReprocessingJobConfig {

    private final ProjectImageRepository projectImageRepository;
    private final ArticleImageRepository articleImageRepository;
    private final ImageProcessorService imageProcessorService;

    @Value("${batch.image-reprocessing.chunk-size:10}")
    private int chunkSize;

    // ========== Job Definition ==========

    @Bean
    public Job imageReprocessingJob(JobRepository jobRepository,
                                    Step reprocessProjectImagesStep,
                                    Step reprocessArticleImagesStep) {
        return new JobBuilder("imageReprocessingJob", jobRepository)
                .start(reprocessProjectImagesStep)
                .next(reprocessArticleImagesStep)
                .build();
    }

    // ========== Project Images Step ==========

    @Bean
    public Step reprocessProjectImagesStep(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager) {
        return new StepBuilder("reprocessProjectImagesStep", jobRepository)
                .<ProjectImage, ReprocessingItem>chunk(chunkSize, transactionManager)
                .reader(projectImageReader())
                .processor(projectImageProcessor())
                .writer(imageReprocessingWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public ItemReader<ProjectImage> projectImageReader() {
        return new ListItemReader<>(new ArrayList<>()) {
            private boolean initialized = false;
            private List<ProjectImage> images;
            private int currentIndex = 0;

            @Override
            public ProjectImage read() {
                if (!initialized) {
                    images = projectImageRepository.findByStatus(ImageStatus.READY);
                    initialized = true;
                    log.info("[BATCH_IMAGE_REPROCESSING] Found project images to reprocess - count={}",
                            images.size());
                }
                if (currentIndex < images.size()) {
                    return images.get(currentIndex++);
                }
                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<ProjectImage, ReprocessingItem> projectImageProcessor() {
        return image -> {
            if (!imageProcessorService.hasOriginalFile(image.getImageUrl())) {
                log.warn("[BATCH_IMAGE_REPROCESSING] Skipping project image without original - id={}, url={}",
                        image.getId(), image.getImageUrl());
                return null;
            }

            Path originalPath = imageProcessorService.resolveOriginalPathFromUrl(image.getImageUrl());
            Path optimizedPath = imageProcessorService.resolveOptimizedPathFromUrl(image.getImageUrl());
            Path thumbnailPath = imageProcessorService.resolveThumbnailPathFromUrl(image.getThumbnailUrl());

            boolean isCarousel = image.getDisplayOrder() != null && image.getDisplayOrder() > 0;

            return ReprocessingItem.builder()
                    .imageId(image.getId())
                    .imageType(ImageType.PROJECT)
                    .originalPath(originalPath)
                    .optimizedPath(optimizedPath)
                    .thumbnailPath(thumbnailPath)
                    .isCarousel(isCarousel)
                    .build();
        };
    }

    // ========== Article Images Step ==========

    @Bean
    public Step reprocessArticleImagesStep(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager) {
        return new StepBuilder("reprocessArticleImagesStep", jobRepository)
                .<ArticleImage, ReprocessingItem>chunk(chunkSize, transactionManager)
                .reader(articleImageReader())
                .processor(articleImageProcessor())
                .writer(imageReprocessingWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public ItemReader<ArticleImage> articleImageReader() {
        return new ListItemReader<>(new ArrayList<>()) {
            private boolean initialized = false;
            private List<ArticleImage> images;
            private int currentIndex = 0;

            @Override
            public ArticleImage read() {
                if (!initialized) {
                    images = articleImageRepository.findByStatus(ImageStatus.READY);
                    initialized = true;
                    log.info("[BATCH_IMAGE_REPROCESSING] Found article images to reprocess - count={}",
                            images.size());
                }
                if (currentIndex < images.size()) {
                    return images.get(currentIndex++);
                }
                return null;
            }
        };
    }

    @Bean
    public ItemProcessor<ArticleImage, ReprocessingItem> articleImageProcessor() {
        return image -> {
            if (!imageProcessorService.hasOriginalFile(image.getImageUrl())) {
                log.warn("[BATCH_IMAGE_REPROCESSING] Skipping article image without original - id={}, url={}",
                        image.getId(), image.getImageUrl());
                return null;
            }

            Path originalPath = imageProcessorService.resolveOriginalPathFromUrl(image.getImageUrl());
            Path optimizedPath = imageProcessorService.resolveOptimizedPathFromUrl(image.getImageUrl());
            Path thumbnailPath = imageProcessorService.resolveThumbnailPathFromUrl(image.getThumbnailUrl());

            return ReprocessingItem.builder()
                    .imageId(image.getId())
                    .imageType(ImageType.ARTICLE)
                    .originalPath(originalPath)
                    .optimizedPath(optimizedPath)
                    .thumbnailPath(thumbnailPath)
                    .isCarousel(false)
                    .build();
        };
    }

    // ========== Shared Writer ==========

    @Bean
    public ItemWriter<ReprocessingItem> imageReprocessingWriter() {
        return items -> {
            for (ReprocessingItem item : items) {
                try {
                    imageProcessorService.reprocessFromOriginal(
                            item.getOriginalPath(),
                            item.getOptimizedPath(),
                            item.getThumbnailPath(),
                            item.isCarousel()
                    );
                    log.info("[BATCH_IMAGE_REPROCESSING] Reprocessed image - type={}, id={}",
                            item.getImageType(), item.getImageId());
                } catch (Exception e) {
                    log.error("[BATCH_IMAGE_REPROCESSING] Failed to reprocess image - type={}, id={}, error={}",
                            item.getImageType(), item.getImageId(), e.getMessage());
                    throw e;
                }
            }
        };
    }

    // ========== Supporting Types ==========

    /**
     * Image type enum for logging and processing differentiation.
     */
    public enum ImageType {
        PROJECT,
        ARTICLE
    }

    /**
     * DTO for carrying reprocessing information between batch steps.
     */
    @lombok.Builder
    @lombok.Getter
    public static class ReprocessingItem {
        private final Long imageId;
        private final ImageType imageType;
        private final Path originalPath;
        private final Path optimizedPath;
        private final Path thumbnailPath;
        private final boolean isCarousel;
    }
}
