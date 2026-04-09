package com.emmanuelgabe.portfolio.moderation;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AI-powered contact moderation service using Google Gemini via Spring AI.
 * Analyzes messages for spam, toxicity, and professional relevance,
 * and assigns a category (MISSION, QUESTION, FEEDBACK, SPAM, INAPPROPRIATE).
 *
 * <p>Follows the fail-open contract: any AI failure (timeout, parsing error,
 * API unavailability) results in automatic approval to avoid blocking
 * legitimate contact attempts.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AiContactModerationService implements ContactModerationService {

    private final ExecutorService moderationExecutor = Executors.newFixedThreadPool(2);

    private final ChatClient chatClient;
    private final ModerationPromptBuilder promptBuilder;
    private final BusinessMetrics metrics;

    @PreDestroy
    void shutdown() {
        moderationExecutor.shutdownNow();
    }

    @Value("${ai.moderation.timeout-seconds:5}")
    private int timeoutSeconds;

    @Override
    public ModerationResult moderate(ContactRequest request) {
        Timer.Sample sample = Timer.start();
        try {
            log.debug("[MODERATION] Starting AI analysis - email={}", request.getEmail());

            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(request);

            AiModerationResponse aiResponse = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .entity(AiModerationResponse.class),
                    moderationExecutor
            ).get(timeoutSeconds, TimeUnit.SECONDS);

            ModerationResult result = convertToResult(aiResponse, request.getEmail());
            recordMetrics(result);
            return result;

        } catch (Exception e) {
            log.error("[MODERATION] AI analysis failed, auto-approving - email={}, error={}",
                    request.getEmail(), e.getMessage());
            metrics.recordModerationFailure();
            return ModerationResult.approved();
        } finally {
            sample.stop(metrics.getModerationTimer());
        }
    }

    private void recordMetrics(ModerationResult result) {
        if (result.verdict() == ModerationVerdict.APPROVED) {
            metrics.recordModerationApproval();
        } else {
            metrics.recordModerationRejection();
        }
        if (result.category() != null) {
            metrics.recordModerationCategory(result.category().name());
        }
    }

    private ModerationResult convertToResult(AiModerationResponse aiResponse, String email) {
        if (aiResponse == null) {
            log.warn("[MODERATION] AI returned null response, auto-approving - email={}", email);
            return ModerationResult.approved();
        }

        String verdict = aiResponse.getVerdict();
        if (verdict == null
                || (!ModerationVerdict.APPROVED.name().equalsIgnoreCase(verdict)
                    && !ModerationVerdict.REJECTED.name().equalsIgnoreCase(verdict))) {
            log.warn("[MODERATION] AI returned invalid verdict='{}', auto-approving - email={}",
                    verdict, email);
            return ModerationResult.approved();
        }

        double confidence = clampConfidence(aiResponse.getConfidenceScore());
        String reason = aiResponse.getReason() != null ? aiResponse.getReason() : "No reason provided";
        ContactCategory category = parseCategory(aiResponse.getCategory());

        ModerationVerdict moderationVerdict = ModerationVerdict.APPROVED.name().equalsIgnoreCase(verdict)
                ? ModerationVerdict.APPROVED
                : ModerationVerdict.REJECTED;

        ModerationResult result = new ModerationResult(moderationVerdict, category, reason, confidence);

        log.info("[MODERATION] AI analysis complete - verdict={}, category={}, reason={}, confidence={}, email={}",
                result.verdict(), result.category(), result.reason(), confidence, email);

        return result;
    }

    private static ContactCategory parseCategory(String categoryStr) {
        if (categoryStr == null) {
            return null;
        }
        try {
            return ContactCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double clampConfidence(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }
}
