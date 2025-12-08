package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.entity.ImageStatus;
import com.emmanuelgabe.portfolio.repository.ArticleImageRepository;
import com.emmanuelgabe.portfolio.repository.ProjectImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminBatchController.
 */
@WebMvcTest(AdminBatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {"batch.enabled=true"})
@ActiveProfiles("test")
class AdminBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean
    private JobExplorer jobExplorer;

    @MockitoBean
    private Job imageReprocessingJob;

    @MockitoBean
    private ProjectImageRepository projectImageRepository;

    @MockitoBean
    private ArticleImageRepository articleImageRepository;

    // ========== Stats Endpoint Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnStats_when_getReprocessingStatsCalledByAdmin() throws Exception {
        // Arrange
        when(projectImageRepository.countByStatus(ImageStatus.READY)).thenReturn(10L);
        when(articleImageRepository.countByStatus(ImageStatus.READY)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/batch/image-reprocessing/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectImagesEligible").value(10))
                .andExpect(jsonPath("$.articleImagesEligible").value(5))
                .andExpect(jsonPath("$.totalEligible").value(15))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnZeroStats_when_noImagesEligible() throws Exception {
        // Arrange
        when(projectImageRepository.countByStatus(ImageStatus.READY)).thenReturn(0L);
        when(articleImageRepository.countByStatus(ImageStatus.READY)).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/batch/image-reprocessing/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEligible").value(0));
    }

    // ========== Run Job Endpoint Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnJobDetails_when_runImageReprocessingJobCalledByAdmin() throws Exception {
        // Arrange
        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getJobId()).thenReturn(123L);
        when(mockExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExecution);

        // Act & Assert
        mockMvc.perform(post("/api/admin/batch/image-reprocessing/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(123))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.exitCode").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnError_when_runJobFails() throws Exception {
        // Arrange
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Job execution failed"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/batch/image-reprocessing/run"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to run batch job"))
                .andExpect(jsonPath("$.message").value("Job execution failed"));
    }

    // ========== Last Job Endpoint Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnLastJobInfo_when_getLastJobExecutionCalledWithExistingJob() throws Exception {
        // Arrange
        JobInstance mockInstance = mock(JobInstance.class);
        when(mockInstance.getInstanceId()).thenReturn(1L);

        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getJobId()).thenReturn(123L);
        when(mockExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(mockExecution.getStartTime()).thenReturn(LocalDateTime.of(2025, 1, 15, 10, 30));
        when(mockExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

        StepExecution mockStep = mock(StepExecution.class);
        when(mockStep.getWriteCount()).thenReturn(15L);
        when(mockStep.getSkipCount()).thenReturn(2L);
        when(mockExecution.getStepExecutions()).thenReturn(Collections.singleton(mockStep));

        when(jobExplorer.findJobInstancesByJobName("imageReprocessingJob", 0, 1))
                .thenReturn(List.of(mockInstance));
        when(jobExplorer.getJobExecutions(mockInstance)).thenReturn(List.of(mockExecution));

        // Act & Assert
        mockMvc.perform(get("/api/admin/batch/image-reprocessing/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastJobId").value(123))
                .andExpect(jsonPath("$.lastJobStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.exitCode").value("COMPLETED"))
                .andExpect(jsonPath("$.processedCount").value(15))
                .andExpect(jsonPath("$.errorCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnNullValues_when_getLastJobExecutionCalledWithNoJobs() throws Exception {
        // Arrange
        when(jobExplorer.findJobInstancesByJobName("imageReprocessingJob", 0, 1))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/admin/batch/image-reprocessing/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastJobId").isEmpty())
                .andExpect(jsonPath("$.lastJobStatus").isEmpty())
                .andExpect(jsonPath("$.lastJobDate").isEmpty())
                .andExpect(jsonPath("$.processedCount").value(0))
                .andExpect(jsonPath("$.errorCount").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_returnInstanceOnly_when_getLastJobExecutionCalledWithNoExecutions() throws Exception {
        // Arrange
        JobInstance mockInstance = mock(JobInstance.class);
        when(mockInstance.getInstanceId()).thenReturn(1L);

        when(jobExplorer.findJobInstancesByJobName("imageReprocessingJob", 0, 1))
                .thenReturn(List.of(mockInstance));
        when(jobExplorer.getJobExecutions(mockInstance)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/admin/batch/image-reprocessing/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastJobId").value(1))
                .andExpect(jsonPath("$.lastJobStatus").isEmpty())
                .andExpect(jsonPath("$.processedCount").value(0));
    }
}
