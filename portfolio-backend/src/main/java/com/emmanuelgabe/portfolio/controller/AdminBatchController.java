package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing batch jobs.
 * Provides endpoints to trigger and monitor batch job executions.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = false)
public class AdminBatchController {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job imageReprocessingJob;
    private final ProjectImageRepository projectImageRepository;
    private final ArticleImageRepository articleImageRepository;

    /**
     * Get statistics about images eligible for reprocessing.
     *
     * @return statistics about project and article images
     */
    @GetMapping("/image-reprocessing/stats")
    public ResponseEntity<Map<String, Object>> getReprocessingStats() {
        log.info("[GET_BATCH_STATS] Fetching image reprocessing statistics");

        long projectImagesReady = projectImageRepository.countByStatus(ImageStatus.READY);
        long articleImagesReady = articleImageRepository.countByStatus(ImageStatus.READY);

        Map<String, Object> stats = new HashMap<>();
        stats.put("projectImagesEligible", projectImagesReady);
        stats.put("articleImagesEligible", articleImagesReady);
        stats.put("totalEligible", projectImagesReady + articleImagesReady);
        stats.put("timestamp", LocalDateTime.now());

        log.info("[GET_BATCH_STATS] Statistics retrieved - projectImages={}, articleImages={}",
                projectImagesReady, articleImagesReady);

        return ResponseEntity.ok(stats);
    }

    /**
     * Trigger the image reprocessing batch job.
     * Reprocesses all READY images from their original files with current quality settings.
     *
     * @return job execution details
     */
    @PostMapping("/image-reprocessing/run")
    public ResponseEntity<Map<String, Object>> runImageReprocessingJob() {
        log.info("[RUN_BATCH_JOB] Starting image reprocessing job");

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("triggeredBy", "admin-api")
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(imageReprocessingJob, params);

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", execution.getJobId());
            response.put("status", execution.getStatus().toString());
            response.put("startTime", execution.getStartTime());
            response.put("exitCode", execution.getExitStatus().getExitCode());

            log.info("[RUN_BATCH_JOB] Job completed - jobId={}, status={}, exitCode={}",
                    execution.getJobId(), execution.getStatus(), execution.getExitStatus().getExitCode());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[RUN_BATCH_JOB] Failed to run job - error={}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to run batch job");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get information about the last executed image reprocessing job.
     *
     * @return last job execution details or empty if no job has been run
     */
    @GetMapping("/image-reprocessing/last")
    public ResponseEntity<Map<String, Object>> getLastJobExecution() {
        log.info("[GET_LAST_JOB] Fetching last image reprocessing job");

        Map<String, Object> response = new HashMap<>();

        List<JobInstance> instances = jobExplorer.findJobInstancesByJobName(
                "imageReprocessingJob", 0, 1);

        if (instances.isEmpty()) {
            response.put("lastJobId", null);
            response.put("lastJobStatus", null);
            response.put("lastJobDate", null);
            response.put("processedCount", 0);
            response.put("errorCount", 0);
            log.info("[GET_LAST_JOB] No job execution found");
            return ResponseEntity.ok(response);
        }

        JobInstance lastInstance = instances.get(0);
        List<JobExecution> executions = jobExplorer.getJobExecutions(lastInstance);

        if (executions.isEmpty()) {
            response.put("lastJobId", lastInstance.getInstanceId());
            response.put("lastJobStatus", null);
            response.put("lastJobDate", null);
            response.put("processedCount", 0);
            response.put("errorCount", 0);
            return ResponseEntity.ok(response);
        }

        JobExecution lastExecution = executions.get(0);

        response.put("lastJobId", lastExecution.getJobId());
        response.put("lastJobStatus", lastExecution.getStatus().toString());
        response.put("lastJobDate", lastExecution.getStartTime());
        response.put("exitCode", lastExecution.getExitStatus().getExitCode());

        // Get processed count from step executions
        long processedCount = lastExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum();
        long errorCount = lastExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getSkipCount())
                .sum();

        response.put("processedCount", processedCount);
        response.put("errorCount", errorCount);

        log.info("[GET_LAST_JOB] Last job found - jobId={}, status={}, processed={}",
                lastExecution.getJobId(), lastExecution.getStatus(), processedCount);

        return ResponseEntity.ok(response);
    }
}
