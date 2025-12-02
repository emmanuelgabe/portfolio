package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminSiteConfigurationController (admin endpoints).
 */
@WebMvcTest(AdminSiteConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminSiteConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SiteConfigurationService siteConfigurationService;

    private SiteConfigurationResponse testConfigResponse;
    private UpdateSiteConfigurationRequest updateRequest;

    @BeforeEach
    void setUp() {
        testConfigResponse = new SiteConfigurationResponse();
        testConfigResponse.setId(1L);
        testConfigResponse.setFullName("Emmanuel Gabe");
        testConfigResponse.setEmail("contact@emmanuelgabe.com");
        testConfigResponse.setHeroTitle("Developpeur Backend");
        testConfigResponse.setHeroDescription("Je cree des applications web modernes.");
        testConfigResponse.setSiteTitle("Portfolio - Emmanuel Gabe");
        testConfigResponse.setSeoDescription("Portfolio de Emmanuel Gabe.");
        testConfigResponse.setProfileImageUrl(null);
        testConfigResponse.setGithubUrl("https://github.com/emmanuelgabe");
        testConfigResponse.setLinkedinUrl("https://linkedin.com/in/egabe");
        testConfigResponse.setCreatedAt(LocalDateTime.now());
        testConfigResponse.setUpdatedAt(LocalDateTime.now());

        updateRequest = new UpdateSiteConfigurationRequest();
        updateRequest.setFullName("New Name");
        updateRequest.setEmail("new@email.com");
        updateRequest.setHeroTitle("New Hero Title");
        updateRequest.setHeroDescription("New Hero Description");
        updateRequest.setSiteTitle("New Site Title");
        updateRequest.setSeoDescription("New SEO Description");
        updateRequest.setGithubUrl("https://github.com/newuser");
        updateRequest.setLinkedinUrl("https://linkedin.com/in/newuser");
    }

    // ========== GET /api/admin/configuration ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_getConfigurationCalledByAdmin() throws Exception {
        // Arrange
        when(siteConfigurationService.getSiteConfiguration()).thenReturn(testConfigResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/configuration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("Emmanuel Gabe")))
                .andExpect(jsonPath("$.heroTitle", is("Developpeur Backend")));

        verify(siteConfigurationService).getSiteConfiguration();
    }

    // ========== PUT /api/admin/configuration ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_updateConfigurationCalledWithValidRequest() throws Exception {
        // Arrange
        SiteConfigurationResponse updatedResponse = new SiteConfigurationResponse();
        updatedResponse.setId(1L);
        updatedResponse.setFullName("New Name");
        updatedResponse.setEmail("new@email.com");
        updatedResponse.setHeroTitle("New Hero Title");
        updatedResponse.setHeroDescription("New Hero Description");
        updatedResponse.setSiteTitle("New Site Title");
        updatedResponse.setSeoDescription("New SEO Description");
        updatedResponse.setGithubUrl("https://github.com/newuser");
        updatedResponse.setLinkedinUrl("https://linkedin.com/in/newuser");
        updatedResponse.setCreatedAt(LocalDateTime.now());
        updatedResponse.setUpdatedAt(LocalDateTime.now());

        when(siteConfigurationService.updateSiteConfiguration(any(UpdateSiteConfigurationRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("New Name")))
                .andExpect(jsonPath("$.heroTitle", is("New Hero Title")));

        verify(siteConfigurationService).updateSiteConfiguration(any(UpdateSiteConfigurationRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return400_when_updateConfigurationCalledWithInvalidEmail() throws Exception {
        // Arrange
        updateRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(put("/api/admin/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return400_when_updateConfigurationCalledWithInvalidUrl() throws Exception {
        // Arrange
        updateRequest.setGithubUrl("not-a-valid-url");

        // Act & Assert
        mockMvc.perform(put("/api/admin/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return400_when_updateConfigurationCalledWithBlankFullName() throws Exception {
        // Arrange
        updateRequest.setFullName("");

        // Act & Assert
        mockMvc.perform(put("/api/admin/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_updateConfigurationCalledAndNotFound() throws Exception {
        // Arrange
        when(siteConfigurationService.updateSiteConfiguration(any(UpdateSiteConfigurationRequest.class)))
                .thenThrow(new ResourceNotFoundException("SiteConfiguration not found with id: 1"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(siteConfigurationService).updateSiteConfiguration(any(UpdateSiteConfigurationRequest.class));
    }

    // ========== POST /api/admin/configuration/profile-image ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_uploadProfileImageCalledWithValidImage() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());
        testConfigResponse.setProfileImageUrl("/uploads/projects/profile_20241201_120000.webp");

        when(siteConfigurationService.uploadProfileImage(any())).thenReturn(testConfigResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/configuration/profile-image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.profileImageUrl", is("/uploads/projects/profile_20241201_120000.webp")));

        verify(siteConfigurationService).uploadProfileImage(any());
    }

    // ========== DELETE /api/admin/configuration/profile-image ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_deleteProfileImageCalled() throws Exception {
        // Arrange
        testConfigResponse.setProfileImageUrl(null);
        when(siteConfigurationService.deleteProfileImage()).thenReturn(testConfigResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/configuration/profile-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist());

        verify(siteConfigurationService).deleteProfileImage();
    }
}
