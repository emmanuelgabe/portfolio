package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminExperienceController (admin endpoints).
 */
@WebMvcTest(AdminExperienceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminExperienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExperienceService experienceService;

    private ExperienceResponse testExperienceResponse;
    private CreateExperienceRequest createRequest;
    private UpdateExperienceRequest updateRequest;

    @BeforeEach
    void setUp() {
        testExperienceResponse = new ExperienceResponse();
        testExperienceResponse.setId(1L);
        testExperienceResponse.setCompany("Test Company");
        testExperienceResponse.setRole("Software Engineer");
        testExperienceResponse.setStartDate(LocalDate.of(2022, 1, 1));
        testExperienceResponse.setEndDate(LocalDate.of(2023, 12, 31));
        testExperienceResponse.setDescription("Test description");
        testExperienceResponse.setType(ExperienceType.WORK);
        testExperienceResponse.setCreatedAt(LocalDateTime.now());
        testExperienceResponse.setUpdatedAt(LocalDateTime.now());
        testExperienceResponse.setOngoing(false);

        createRequest = new CreateExperienceRequest();
        createRequest.setCompany("New Company");
        createRequest.setRole("Developer");
        createRequest.setStartDate(LocalDate.of(2023, 1, 1));
        createRequest.setDescription("New experience description");
        createRequest.setType(ExperienceType.WORK);

        updateRequest = new UpdateExperienceRequest();
        updateRequest.setCompany("Updated Company");
        updateRequest.setRole("Senior Developer");
    }

    // ========== Get All Experiences Tests ==========

    @Test
    void should_return200AndListOfExperiences_when_getAllExperiencesCalled() throws Exception {
        // Arrange
        List<ExperienceResponse> experiences = List.of(testExperienceResponse);
        when(experienceService.getAllExperiences()).thenReturn(experiences);

        // Act & Assert
        mockMvc.perform(get("/api/admin/experiences"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].company", is("Test Company")))
                .andExpect(jsonPath("$[0].role", is("Software Engineer")));

        verify(experienceService).getAllExperiences();
    }

    @Test
    void should_return200AndEmptyList_when_getAllExperiencesCalledWithNoExperiences() throws Exception {
        // Arrange
        when(experienceService.getAllExperiences()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/experiences"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(experienceService).getAllExperiences();
    }

    // ========== Get Experience By ID Tests ==========

    @Test
    void should_return200AndExperience_when_getExperienceByIdCalledWithExistingId() throws Exception {
        // Arrange
        when(experienceService.getExperienceById(1L)).thenReturn(testExperienceResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/experiences/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.company", is("Test Company")))
                .andExpect(jsonPath("$.role", is("Software Engineer")))
                .andExpect(jsonPath("$.type", is("WORK")));

        verify(experienceService).getExperienceById(1L);
    }

    @Test
    void should_return404_when_getExperienceByIdCalledWithNonExistingId() throws Exception {
        // Arrange
        when(experienceService.getExperienceById(999L))
                .thenThrow(new ResourceNotFoundException("Experience", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/admin/experiences/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Experience not found")));

        verify(experienceService).getExperienceById(999L);
    }

    // ========== Create Experience Tests ==========

    @Test
    void should_return201AndCreatedExperience_when_createExperienceCalledWithValidRequest() throws Exception {
        // Arrange
        when(experienceService.createExperience(any(CreateExperienceRequest.class)))
                .thenReturn(testExperienceResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.company", is("Test Company")));

        verify(experienceService).createExperience(any(CreateExperienceRequest.class));
    }

    @Test
    void should_return400_when_createExperienceCalledWithInvalidRequest() throws Exception {
        // Arrange - empty request body
        CreateExperienceRequest invalidRequest = new CreateExperienceRequest();

        // Act & Assert
        mockMvc.perform(post("/api/admin/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== Update Experience Tests ==========

    @Test
    void should_return200AndUpdatedExperience_when_updateExperienceCalledWithValidRequest() throws Exception {
        // Arrange
        ExperienceResponse updatedResponse = new ExperienceResponse();
        updatedResponse.setId(1L);
        updatedResponse.setCompany("Updated Company");
        updatedResponse.setRole("Senior Developer");
        updatedResponse.setType(ExperienceType.WORK);
        updatedResponse.setStartDate(LocalDate.of(2022, 1, 1));
        updatedResponse.setCreatedAt(LocalDateTime.now());
        updatedResponse.setUpdatedAt(LocalDateTime.now());

        when(experienceService.updateExperience(eq(1L), any(UpdateExperienceRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/experiences/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.company", is("Updated Company")))
                .andExpect(jsonPath("$.role", is("Senior Developer")));

        verify(experienceService).updateExperience(eq(1L), any(UpdateExperienceRequest.class));
    }

    @Test
    void should_return404_when_updateExperienceCalledWithNonExistingId() throws Exception {
        // Arrange
        when(experienceService.updateExperience(eq(999L), any(UpdateExperienceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Experience", "id", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/admin/experiences/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(experienceService).updateExperience(eq(999L), any(UpdateExperienceRequest.class));
    }

    // ========== Delete Experience Tests ==========

    @Test
    void should_return204_when_deleteExperienceCalledWithExistingId() throws Exception {
        // Arrange
        doNothing().when(experienceService).deleteExperience(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/experiences/1"))
                .andExpect(status().isNoContent());

        verify(experienceService).deleteExperience(1L);
    }

    @Test
    void should_return404_when_deleteExperienceCalledWithNonExistingId() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Experience", "id", 999L))
                .when(experienceService).deleteExperience(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/experiences/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(experienceService).deleteExperience(999L);
    }
}
