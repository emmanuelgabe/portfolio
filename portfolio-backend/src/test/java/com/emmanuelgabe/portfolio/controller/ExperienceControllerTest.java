package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ExperienceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ExperienceController (public endpoints only).
 * Admin endpoint tests are in AdminExperienceControllerTest.
 */
@WebMvcTest(ExperienceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class ExperienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExperienceService experienceService;

    private ExperienceResponse testExperienceResponse;

    @BeforeEach
    void setUp() {
        testExperienceResponse = new ExperienceResponse();
        testExperienceResponse.setId(1L);
        testExperienceResponse.setCompany("Test Company");
        testExperienceResponse.setRole("Software Engineer");
        testExperienceResponse.setStartDate(LocalDate.of(2022, 1, 1));
        testExperienceResponse.setEndDate(LocalDate.of(2023, 12, 31));
        testExperienceResponse.setDescription("Test description for the experience");
        testExperienceResponse.setType(ExperienceType.WORK);
        testExperienceResponse.setShowMonths(true);
        testExperienceResponse.setCreatedAt(LocalDateTime.now());

        testExperienceResponse.setUpdatedAt(LocalDateTime.now());
        testExperienceResponse.setOngoing(false);
    }

    @Test
    void should_return200AndListOfExperiences_when_getAllExperiencesCalled() throws Exception {
        List<ExperienceResponse> experiences = List.of(testExperienceResponse);
        when(experienceService.getAllExperiences()).thenReturn(experiences);

        mockMvc.perform(get("/api/experiences"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].company", is("Test Company")))
                .andExpect(jsonPath("$[0].role", is("Software Engineer")))
                .andExpect(jsonPath("$[0].type", is("WORK")));

        verify(experienceService).getAllExperiences();
    }

    @Test
    void should_return200AndExperience_when_getExperienceByIdWithValidId() throws Exception {
        when(experienceService.getExperienceById(1L)).thenReturn(testExperienceResponse);

        mockMvc.perform(get("/api/experiences/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.company", is("Test Company")))
                .andExpect(jsonPath("$.role", is("Software Engineer")));

        verify(experienceService).getExperienceById(1L);
    }

    @Test
    void should_return404_when_getExperienceByIdWithInvalidId() throws Exception {
        when(experienceService.getExperienceById(999L))
                .thenThrow(new ResourceNotFoundException("Experience", "id", 999L));

        mockMvc.perform(get("/api/experiences/999"))
                .andExpect(status().isNotFound());

        verify(experienceService).getExperienceById(999L);
    }

    @Test
    void should_return200AndFilteredExperiences_when_getExperiencesByType() throws Exception {
        List<ExperienceResponse> experiences = List.of(testExperienceResponse);
        when(experienceService.getExperiencesByType(ExperienceType.WORK)).thenReturn(experiences);

        mockMvc.perform(get("/api/experiences/type/WORK"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("WORK")));

        verify(experienceService).getExperiencesByType(ExperienceType.WORK);
    }

    @Test
    void should_return200AndOngoingExperiences_when_getOngoingExperiences() throws Exception {
        ExperienceResponse ongoingExp = new ExperienceResponse();
        ongoingExp.setId(2L);
        ongoingExp.setCompany("Current Company");
        ongoingExp.setRole("Current Role");
        ongoingExp.setStartDate(LocalDate.of(2023, 1, 1));
        ongoingExp.setEndDate(null);
        ongoingExp.setDescription("Ongoing experience description");
        ongoingExp.setType(ExperienceType.WORK);
        ongoingExp.setShowMonths(true);
        ongoingExp.setOngoing(true);


        List<ExperienceResponse> experiences = List.of(ongoingExp);
        when(experienceService.getOngoingExperiences()).thenReturn(experiences);

        mockMvc.perform(get("/api/experiences/ongoing"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ongoing", is(true)));

        verify(experienceService).getOngoingExperiences();
    }

    @Test
    void should_return200AndRecentExperiences_when_getRecentExperiencesWithLimit() throws Exception {
        List<ExperienceResponse> experiences = List.of(testExperienceResponse);
        when(experienceService.getRecentExperiences(3)).thenReturn(experiences);

        mockMvc.perform(get("/api/experiences/recent?limit=3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));

        verify(experienceService).getRecentExperiences(3);
    }

    @Test
    void should_return200AndRecentExperiencesWithDefaultLimit_when_getRecentExperiencesWithoutLimit() throws Exception {
        List<ExperienceResponse> experiences = List.of(testExperienceResponse);
        when(experienceService.getRecentExperiences(3)).thenReturn(experiences);

        mockMvc.perform(get("/api/experiences/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));

        verify(experienceService).getRecentExperiences(3);
    }
}
