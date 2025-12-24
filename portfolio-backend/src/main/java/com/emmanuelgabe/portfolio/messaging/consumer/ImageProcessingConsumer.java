package com.emmanuelgabe.portfolio.messaging.consumer;

import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.messaging.event.ImageProcessingEvent;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import com.emmanuelgabe.portfolio.service.ImageProcessorService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer for image processing events from RabbitMQ queue.
 * Processes image events asynchronously using ImageProcessorService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class ImageProcessingConsumer {

    private final ImageProcessorService imageProcessorService;
    private final ProjectImageRepository projectImageRepository;
    private final ArticleImageRepository articleImageRepository;
    private final BusinessMetrics metrics;

    /**
     * Handle image processing events from the queue.
     * Retry is handled by Spring AMQP configuration.
     *
     * @param event the image processing event to process
     */
    @RabbitListener(queues = "${rabbitmq.queues.image}")
    @Transactional
    public void handleImageProcessingEvent(ImageProcessingEvent event) {
        log.info("[IMAGE_CONSUMER] Processing image event - eventId={}, type={}, entityId={}, "
                + "projectImageId={}, articleImageId={}",
                event.getEventId(), event.getProcessingType(), event.getEntityId(),
                event.getProjectImageId(), event.getArticleImageId());

        Timer.Sample sample = Timer.start();
        try {
            imageProcessorService.processImage(event);

            // Update status to READY after successful processing
            updateImageStatusToReady(event);

            sample.stop(metrics.getImageProcessingTimer());
            metrics.recordImageProcessed();

            log.info("[IMAGE_CONSUMER] Image processed - eventId={}, type={}",
                    event.getEventId(), event.getProcessingType());

        } catch (Exception e) {
            // Update status to FAILED on error
            updateImageStatusToFailed(event);

            metrics.recordImageProcessingFailure();
            log.error("[IMAGE_CONSUMER] Failed to process image - eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
            throw e;
        }
    }

    private void updateImageStatusToReady(ImageProcessingEvent event) {
        if (event.getProjectImageId() != null) {
            int updated = projectImageRepository.updateStatus(event.getProjectImageId(), ImageStatus.READY);
            log.debug("[IMAGE_CONSUMER] Status updated to READY - projectImageId={}, rowsAffected={}",
                    event.getProjectImageId(), updated);
        }
        if (event.getArticleImageId() != null) {
            int updated = articleImageRepository.updateStatus(event.getArticleImageId(), ImageStatus.READY);
            log.debug("[IMAGE_CONSUMER] Status updated to READY - articleImageId={}, rowsAffected={}",
                    event.getArticleImageId(), updated);
        }
    }

    private void updateImageStatusToFailed(ImageProcessingEvent event) {
        if (event.getProjectImageId() != null) {
            try {
                projectImageRepository.updateStatus(event.getProjectImageId(), ImageStatus.FAILED);
                log.warn("[IMAGE_CONSUMER] Status updated to FAILED - projectImageId={}",
                        event.getProjectImageId());
            } catch (Exception statusException) {
                log.error("[IMAGE_CONSUMER] Failed to update status to FAILED - projectImageId={}",
                        event.getProjectImageId(), statusException);
            }
        }
        if (event.getArticleImageId() != null) {
            try {
                articleImageRepository.updateStatus(event.getArticleImageId(), ImageStatus.FAILED);
                log.warn("[IMAGE_CONSUMER] Status updated to FAILED - articleImageId={}",
                        event.getArticleImageId());
            } catch (Exception statusException) {
                log.error("[IMAGE_CONSUMER] Failed to update status to FAILED - articleImageId={}",
                        event.getArticleImageId(), statusException);
            }
        }
    }
}
