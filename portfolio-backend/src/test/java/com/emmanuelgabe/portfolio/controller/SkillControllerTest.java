package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.SkillService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for SkillController (public endpoints only).
 * Admin endpoint tests are in AdminSkillControllerTest.
 */
@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillService skillService;

    private SkillResponse testSkillResponse;

    @BeforeEach
    void setUp() {
        testSkillResponse = new SkillResponse();
        testSkillResponse.setId(1L);
        testSkillResponse.setName("Spring Boot");
        testSkillResponse.setIcon("bi-spring");
        testSkillResponse.setColor("#6DB33F");
        testSkillResponse.setCategory(SkillCategory.BACKEND);
        testSkillResponse.setCategoryDisplayName("Backend");
        testSkillResponse.setDisplayOrder(1);
        testSkillResponse.setCreatedAt(LocalDateTime.now());
        testSkillResponse.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void shouldReturn200AndListOfSkills_whenGetAllSkills() throws Exception {
        List<SkillResponse> skills = List.of(testSkillResponse);
        when(skillService.getAllSkills()).thenReturn(skills);

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Spring Boot")))
                .andExpect(jsonPath("$[0].icon", is("bi-spring")))
                .andExpect(jsonPath("$[0].color", is("#6DB33F")))
                .andExpect(jsonPath("$[0].category", is("BACKEND")))
                .andExpect(jsonPath("$[0].displayOrder", is(1)));

        verify(skillService).getAllSkills();
    }

    @Test
    void shouldReturn200AndEmptyList_whenGetAllSkillsAndNoSkillsExist() throws Exception {
        when(skillService.getAllSkills()).thenReturn(List.of());

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(skillService).getAllSkills();
    }

    @Test
    void shouldReturn200AndSkill_whenGetSkillByIdAndSkillExists() throws Exception {
        when(skillService.getSkillById(1L)).thenReturn(testSkillResponse);

        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Spring Boot")))
                .andExpect(jsonPath("$.category", is("BACKEND")));

        verify(skillService).getSkillById(1L);
    }

    @Test
    void shouldReturn404_whenGetSkillByIdAndSkillNotFound() throws Exception {
        when(skillService.getSkillById(999L))
                .thenThrow(new ResourceNotFoundException("Skill", "id", 999L));

        mockMvc.perform(get("/api/skills/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Skill not found")));

        verify(skillService).getSkillById(999L);
    }

    @Test
    void shouldReturn200AndFilteredSkills_whenGetSkillsByCategory() throws Exception {
        List<SkillResponse> backendSkills = List.of(testSkillResponse);
        when(skillService.getSkillsByCategory(SkillCategory.BACKEND)).thenReturn(backendSkills);

        mockMvc.perform(get("/api/skills/category/BACKEND"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("BACKEND")));

        verify(skillService).getSkillsByCategory(SkillCategory.BACKEND);
    }
}
