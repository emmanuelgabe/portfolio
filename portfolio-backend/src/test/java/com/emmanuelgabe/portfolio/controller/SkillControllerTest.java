package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SkillService skillService;

    private SkillResponse testSkillResponse;
    private CreateSkillRequest createRequest;
    private UpdateSkillRequest updateRequest;

    @BeforeEach
    void setUp() {
        testSkillResponse = new SkillResponse();
        testSkillResponse.setId(1L);
        testSkillResponse.setName("Spring Boot");
        testSkillResponse.setIcon("bi-spring");
        testSkillResponse.setColor("#6DB33F");
        testSkillResponse.setCategory(SkillCategory.BACKEND);
        testSkillResponse.setCategoryDisplayName("Backend");
        testSkillResponse.setLevel(85);
        testSkillResponse.setDisplayOrder(1);
        testSkillResponse.setCreatedAt(LocalDateTime.now());
        testSkillResponse.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateSkillRequest();
        createRequest.setName("Angular");
        createRequest.setIcon("bi-angular");
        createRequest.setColor("#DD0031");
        createRequest.setCategory(SkillCategory.FRONTEND);
        createRequest.setLevel(80);
        createRequest.setDisplayOrder(2);

        updateRequest = new UpdateSkillRequest();
        updateRequest.setName("Spring Boot Updated");
        updateRequest.setLevel(90);
    }

    @Test
    void shouldReturn200AndListOfSkills_whenGetAllSkills() throws Exception {
        // Given
        List<SkillResponse> skills = Arrays.asList(testSkillResponse);
        when(skillService.getAllSkills()).thenReturn(skills);

        // When & Then
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Spring Boot")))
                .andExpect(jsonPath("$[0].icon", is("bi-spring")))
                .andExpect(jsonPath("$[0].color", is("#6DB33F")))
                .andExpect(jsonPath("$[0].category", is("BACKEND")))
                .andExpect(jsonPath("$[0].level", is(85)))
                .andExpect(jsonPath("$[0].displayOrder", is(1)));

        verify(skillService, times(1)).getAllSkills();
    }

    @Test
    void shouldReturn200AndEmptyList_whenGetAllSkillsAndNoSkillsExist() throws Exception {
        // Given
        when(skillService.getAllSkills()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(skillService, times(1)).getAllSkills();
    }

    @Test
    void shouldReturn200AndSkill_whenGetSkillByIdAndSkillExists() throws Exception {
        // Given
        when(skillService.getSkillById(1L)).thenReturn(testSkillResponse);

        // When & Then
        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Spring Boot")))
                .andExpect(jsonPath("$.category", is("BACKEND")))
                .andExpect(jsonPath("$.level", is(85)));

        verify(skillService, times(1)).getSkillById(1L);
    }

    @Test
    void shouldReturn404_whenGetSkillByIdAndSkillNotFound() throws Exception {
        // Given
        when(skillService.getSkillById(999L))
                .thenThrow(new ResourceNotFoundException("Skill", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/skills/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Skill not found")));

        verify(skillService, times(1)).getSkillById(999L);
    }

    @Test
    void shouldReturn200AndFilteredSkills_whenGetSkillsByCategory() throws Exception {
        // Given
        List<SkillResponse> backendSkills = Arrays.asList(testSkillResponse);
        when(skillService.getSkillsByCategory(SkillCategory.BACKEND)).thenReturn(backendSkills);

        // When & Then
        mockMvc.perform(get("/api/skills/category/BACKEND"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category", is("BACKEND")));

        verify(skillService, times(1)).getSkillsByCategory(SkillCategory.BACKEND);
    }

    @Test
    void shouldReturn201AndCreatedSkill_whenCreateSkillWithValidData() throws Exception {
        // Given
        when(skillService.createSkill(any(CreateSkillRequest.class)))
                .thenReturn(testSkillResponse);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Spring Boot")));

        verify(skillService, times(1)).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithInvalidData() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName("A"); // Too short
        invalidRequest.setIcon(""); // Blank
        invalidRequest.setColor("invalid"); // Invalid hex color
        invalidRequest.setCategory(SkillCategory.BACKEND);
        invalidRequest.setLevel(150); // Exceeds max
        invalidRequest.setDisplayOrder(-1); // Below min

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithBlankName() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName(""); // Blank
        invalidRequest.setIcon("bi-test");
        invalidRequest.setColor("#FF5733");
        invalidRequest.setCategory(SkillCategory.BACKEND);
        invalidRequest.setLevel(80);
        invalidRequest.setDisplayOrder(1);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithInvalidColorFormat() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName("Test Skill");
        invalidRequest.setIcon("bi-test");
        invalidRequest.setColor("red"); // Invalid format - not hex
        invalidRequest.setCategory(SkillCategory.BACKEND);
        invalidRequest.setLevel(80);
        invalidRequest.setDisplayOrder(1);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithLevelBelowMinimum() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName("Test Skill");
        invalidRequest.setIcon("bi-test");
        invalidRequest.setColor("#FF5733");
        invalidRequest.setCategory(SkillCategory.BACKEND);
        invalidRequest.setLevel(-10); // Below minimum
        invalidRequest.setDisplayOrder(1);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.level").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithLevelAboveMaximum() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName("Test Skill");
        invalidRequest.setIcon("bi-test");
        invalidRequest.setColor("#FF5733");
        invalidRequest.setCategory(SkillCategory.BACKEND);
        invalidRequest.setLevel(150); // Above maximum
        invalidRequest.setDisplayOrder(1);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.level").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenCreateSkillWithNullCategory() throws Exception {
        // Given
        CreateSkillRequest invalidRequest = new CreateSkillRequest();
        invalidRequest.setName("Test Skill");
        invalidRequest.setIcon("bi-test");
        invalidRequest.setColor("#FF5733");
        invalidRequest.setCategory(null); // Null category
        invalidRequest.setLevel(80);
        invalidRequest.setDisplayOrder(1);

        // When & Then
        mockMvc.perform(post("/api/skills/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists());

        verify(skillService, never()).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void shouldReturn200AndUpdatedSkill_whenUpdateSkillWithValidData() throws Exception {
        // Given
        when(skillService.updateSkill(eq(1L), any(UpdateSkillRequest.class)))
                .thenReturn(testSkillResponse);

        // When & Then
        mockMvc.perform(put("/api/skills/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Spring Boot")));

        verify(skillService, times(1)).updateSkill(eq(1L), any(UpdateSkillRequest.class));
    }

    @Test
    void shouldReturn404_whenUpdateSkillAndSkillNotFound() throws Exception {
        // Given
        when(skillService.updateSkill(eq(999L), any(UpdateSkillRequest.class)))
                .thenThrow(new ResourceNotFoundException("Skill", "id", 999L));

        // When & Then
        mockMvc.perform(put("/api/skills/admin/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Skill not found")));

        verify(skillService, times(1)).updateSkill(eq(999L), any(UpdateSkillRequest.class));
    }

    @Test
    void shouldReturn400_whenUpdateSkillWithInvalidColorFormat() throws Exception {
        // Given
        UpdateSkillRequest invalidRequest = new UpdateSkillRequest();
        invalidRequest.setColor("not-a-hex-color");

        // When & Then
        mockMvc.perform(put("/api/skills/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").exists());

        verify(skillService, never()).updateSkill(eq(1L), any(UpdateSkillRequest.class));
    }

    @Test
    void shouldReturn204_whenDeleteSkillAndSkillExists() throws Exception {
        // Given
        doNothing().when(skillService).deleteSkill(1L);

        // When & Then
        mockMvc.perform(delete("/api/skills/admin/1"))
                .andExpect(status().isNoContent());

        verify(skillService, times(1)).deleteSkill(1L);
    }

    @Test
    void shouldReturn404_whenDeleteSkillAndSkillNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Skill", "id", 999L))
                .when(skillService).deleteSkill(999L);

        // When & Then
        mockMvc.perform(delete("/api/skills/admin/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Skill not found")));

        verify(skillService, times(1)).deleteSkill(999L);
    }

    @Test
    void shouldIncludeAllFields_whenSerializingSkillToJson() throws Exception {
        // Given
        when(skillService.getSkillById(1L)).thenReturn(testSkillResponse);

        // When & Then
        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.icon").exists())
                .andExpect(jsonPath("$.color").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.categoryDisplayName").exists())
                .andExpect(jsonPath("$.level").exists())
                .andExpect(jsonPath("$.displayOrder").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldFormatDateTimeCorrectly_whenSerializingSkillToJson() throws Exception {
        // Given
        when(skillService.getSkillById(1L)).thenReturn(testSkillResponse);

        // When & Then
        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }
}
