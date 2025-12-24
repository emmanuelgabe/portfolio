package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for SiteConfigurationController (public endpoint).
 * Admin endpoint tests are in AdminSiteConfigurationControllerTest.
 */
@WebMvcTest(SiteConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class SiteConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteConfigurationService siteConfigurationService;

    private SiteConfigurationResponse testConfigResponse;

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
    }

    @Test
    void should_return200AndConfig_when_getSiteConfigurationCalled() throws Exception {
        // Arrange
        when(siteConfigurationService.getSiteConfiguration()).thenReturn(testConfigResponse);

        // Act & Assert
        mockMvc.perform(get("/api/configuration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("Emmanuel Gabe")))
                .andExpect(jsonPath("$.heroTitle", is("Developpeur Backend")))
                .andExpect(jsonPath("$.githubUrl", is("https://github.com/emmanuelgabe")));

        verify(siteConfigurationService).getSiteConfiguration();
    }

    @Test
    void should_return404_when_getSiteConfigurationCalledAndNotFound() throws Exception {
        // Arrange
        when(siteConfigurationService.getSiteConfiguration())
                .thenThrow(new ResourceNotFoundException("SiteConfiguration", "id", 1L));

        // Act & Assert
        mockMvc.perform(get("/api/configuration"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(siteConfigurationService).getSiteConfiguration();
    }
}
