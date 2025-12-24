package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.IconType;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.SkillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminSkillController (admin endpoints).
 */
@WebMvcTest(AdminSkillController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminSkillControllerTest {

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
        testSkillResponse.setName("Java");
        testSkillResponse.setIcon("bi-code");
        testSkillResponse.setIconType(IconType.FONT_AWESOME);
        testSkillResponse.setColor("#007396");
        testSkillResponse.setCategory(SkillCategory.BACKEND);
        testSkillResponse.setCategoryDisplayName("Backend");
        testSkillResponse.setDisplayOrder(1);
        testSkillResponse.setCreatedAt(LocalDateTime.now());
        testSkillResponse.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateSkillRequest();
        createRequest.setName("New Skill");
        createRequest.setIcon("bi-star");
        createRequest.setIconType(IconType.FONT_AWESOME);
        createRequest.setColor("#FF0000");
        createRequest.setCategory(SkillCategory.FRONTEND);
        createRequest.setDisplayOrder(5);

        updateRequest = new UpdateSkillRequest();
        updateRequest.setName("Updated Skill");
        updateRequest.setColor("#00FF00");
    }

    // ========== Get All Skills Tests ==========

    @Test
    void should_return200AndListOfSkills_when_getAllSkillsCalled() throws Exception {
        // Arrange
        List<SkillResponse> skills = List.of(testSkillResponse);
        when(skillService.getAllSkills()).thenReturn(skills);

        // Act & Assert
        mockMvc.perform(get("/api/admin/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Java")))
                .andExpect(jsonPath("$[0].category", is("BACKEND")));

        verify(skillService).getAllSkills();
    }

    @Test
    void should_return200AndEmptyList_when_getAllSkillsCalledWithNoSkills() throws Exception {
        // Arrange
        when(skillService.getAllSkills()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/admin/skills"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(skillService).getAllSkills();
    }

    // ========== Get Skill By ID Tests ==========

    @Test
    void should_return200AndSkill_when_getSkillByIdCalledWithExistingId() throws Exception {
        // Arrange
        when(skillService.getSkillById(1L)).thenReturn(testSkillResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/skills/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Java")))
                .andExpect(jsonPath("$.color", is("#007396")))
                .andExpect(jsonPath("$.category", is("BACKEND")));

        verify(skillService).getSkillById(1L);
    }

    @Test
    void should_return404_when_getSkillByIdCalledWithNonExistingId() throws Exception {
        // Arrange
        when(skillService.getSkillById(999L))
                .thenThrow(new ResourceNotFoundException("Skill", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/admin/skills/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(skillService).getSkillById(999L);
    }

    // ========== Create Skill Tests ==========

    @Test
    void should_return201AndCreatedSkill_when_createSkillCalledWithValidRequest() throws Exception {
        // Arrange
        when(skillService.createSkill(any(CreateSkillRequest.class)))
                .thenReturn(testSkillResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Java")));

        verify(skillService).createSkill(any(CreateSkillRequest.class));
    }

    @Test
    void should_return400_when_createSkillCalledWithInvalidRequest() throws Exception {
        // Arrange - empty request
        CreateSkillRequest invalidRequest = new CreateSkillRequest();

        // Act & Assert
        mockMvc.perform(post("/api/admin/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ========== Update Skill Tests ==========

    @Test
    void should_return200AndUpdatedSkill_when_updateSkillCalledWithValidRequest() throws Exception {
        // Arrange
        SkillResponse updatedResponse = new SkillResponse();
        updatedResponse.setId(1L);
        updatedResponse.setName("Updated Skill");
        updatedResponse.setColor("#00FF00");
        updatedResponse.setCategory(SkillCategory.BACKEND);
        updatedResponse.setCategoryDisplayName("Backend");
        updatedResponse.setIconType(IconType.FONT_AWESOME);
        updatedResponse.setDisplayOrder(1);
        updatedResponse.setCreatedAt(LocalDateTime.now());
        updatedResponse.setUpdatedAt(LocalDateTime.now());

        when(skillService.updateSkill(eq(1L), any(UpdateSkillRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/skills/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Skill")))
                .andExpect(jsonPath("$.color", is("#00FF00")));

        verify(skillService).updateSkill(eq(1L), any(UpdateSkillRequest.class));
    }

    @Test
    void should_return404_when_updateSkillCalledWithNonExistingId() throws Exception {
        // Arrange
        when(skillService.updateSkill(eq(999L), any(UpdateSkillRequest.class)))
                .thenThrow(new ResourceNotFoundException("Skill", "id", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/admin/skills/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(skillService).updateSkill(eq(999L), any(UpdateSkillRequest.class));
    }

    // ========== Delete Skill Tests ==========

    @Test
    void should_return204_when_deleteSkillCalledWithExistingId() throws Exception {
        // Arrange
        doNothing().when(skillService).deleteSkill(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/skills/1"))
                .andExpect(status().isNoContent());

        verify(skillService).deleteSkill(1L);
    }

    @Test
    void should_return404_when_deleteSkillCalledWithNonExistingId() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Skill", "id", 999L))
                .when(skillService).deleteSkill(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/skills/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        verify(skillService).deleteSkill(999L);
    }

    // ========== Icon Upload Tests ==========

    @Test
    void should_return200AndUpdatedSkill_when_uploadSkillIconCalled() throws Exception {
        // Arrange
        MockMultipartFile iconFile = new MockMultipartFile(
                "file",
                "icon.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        SkillResponse responseWithIcon = new SkillResponse();
        responseWithIcon.setId(1L);
        responseWithIcon.setName("Java");
        responseWithIcon.setIconType(IconType.CUSTOM_SVG);
        responseWithIcon.setCustomIconUrl("/uploads/icons/skill_1.svg");
        responseWithIcon.setCategory(SkillCategory.BACKEND);
        responseWithIcon.setCategoryDisplayName("Backend");
        responseWithIcon.setColor("#007396");
        responseWithIcon.setDisplayOrder(1);
        responseWithIcon.setCreatedAt(LocalDateTime.now());
        responseWithIcon.setUpdatedAt(LocalDateTime.now());

        when(skillService.uploadSkillIcon(eq(1L), any()))
                .thenReturn(responseWithIcon);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/skills/1/icon")
                        .file(iconFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.iconType", is("CUSTOM_SVG")))
                .andExpect(jsonPath("$.customIconUrl", is("/uploads/icons/skill_1.svg")));

        verify(skillService).uploadSkillIcon(eq(1L), any());
    }
}
