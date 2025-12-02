package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ProjectImageResponse;
import com.emmanuelgabe.portfolio.dto.ReorderProjectImagesRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectImageRequest;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.ProjectService;
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
import java.util.Arrays;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ProjectImageController.
 */
@WebMvcTest(ProjectImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class ProjectImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    private ProjectImageResponse imageResponse;
    private MockMultipartFile mockFile;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        imageResponse = new ProjectImageResponse(
            1L,
            "/uploads/projects/project_1_img_0.webp",
            "/uploads/projects/project_1_img_0_thumb.webp",
            "Test image alt text",
            "Test caption",
            0,
            true,
            now
        );

        mockFile = new MockMultipartFile(
            "file",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".getBytes()
        );
    }

    // ========== Upload Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return201AndImageResponse_when_uploadImageWithValidFile() throws Exception {
        // Arrange
        when(projectService.addImageToProject(eq(1L), any(), any(), any())).thenReturn(imageResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/projects/1/images")
                .file(mockFile)
                .param("altText", "Test alt")
                .param("caption", "Test caption"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.imageUrl", is("/uploads/projects/project_1_img_0.webp")))
            .andExpect(jsonPath("$.primary", is(true)));

        verify(projectService).addImageToProject(eq(1L), any(), eq("Test alt"), eq("Test caption"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_uploadImageToNonexistentProject() throws Exception {
        // Arrange
        when(projectService.addImageToProject(eq(999L), any(), any(), any()))
            .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/projects/999/images")
                .file(mockFile))
            .andExpect(status().isNotFound());

        verify(projectService).addImageToProject(eq(999L), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return400_when_uploadImageAndLimitReached() throws Exception {
        // Arrange
        when(projectService.addImageToProject(eq(1L), any(), any(), any()))
            .thenThrow(new IllegalStateException("Maximum 10 images allowed per project"));

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/projects/1/images")
                .file(mockFile))
            .andExpect(status().isBadRequest());

        verify(projectService).addImageToProject(eq(1L), any(), any(), any());
    }

    // ========== Get Images Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndImageList_when_getImagesWithValidProject() throws Exception {
        // Arrange
        ProjectImageResponse image2 = new ProjectImageResponse(
            2L, "/uploads/projects/project_1_img_1.webp",
            "/uploads/projects/project_1_img_1_thumb.webp",
            null, null, 1, false, now
        );
        List<ProjectImageResponse> images = Arrays.asList(imageResponse, image2);
        when(projectService.getProjectImages(1L)).thenReturn(images);

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects/1/images"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[1].id", is(2)));

        verify(projectService).getProjectImages(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_getImagesFromNonexistentProject() throws Exception {
        // Arrange
        when(projectService.getProjectImages(999L))
            .thenThrow(new ResourceNotFoundException("Project", "id", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/admin/projects/999/images"))
            .andExpect(status().isNotFound());

        verify(projectService).getProjectImages(999L);
    }

    // ========== Delete Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return204_when_deleteImageWithValidIds() throws Exception {
        // Arrange
        doNothing().when(projectService).removeImageFromProject(1L, 10L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/1/images/10"))
            .andExpect(status().isNoContent());

        verify(projectService).removeImageFromProject(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteImageFromNonexistentProject() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Project", "id", 999L))
            .when(projectService).removeImageFromProject(999L, 10L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/999/images/10"))
            .andExpect(status().isNotFound());

        verify(projectService).removeImageFromProject(999L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteNonexistentImage() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("ProjectImage", "id", 999L))
            .when(projectService).removeImageFromProject(1L, 999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/projects/1/images/999"))
            .andExpect(status().isNotFound());

        verify(projectService).removeImageFromProject(1L, 999L);
    }

    // ========== Update Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndUpdatedImage_when_updateImageWithValidData() throws Exception {
        // Arrange
        UpdateProjectImageRequest request = new UpdateProjectImageRequest("Updated alt", "Updated caption");
        ProjectImageResponse updatedResponse = new ProjectImageResponse(
            1L, "/uploads/projects/project_1_img_0.webp",
            "/uploads/projects/project_1_img_0_thumb.webp",
            "Updated alt", "Updated caption", 0, true, now
        );
        when(projectService.updateProjectImage(eq(1L), eq(10L), any())).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1/images/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.altText", is("Updated alt")))
            .andExpect(jsonPath("$.caption", is("Updated caption")));

        verify(projectService).updateProjectImage(eq(1L), eq(10L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_updateNonexistentImage() throws Exception {
        // Arrange
        UpdateProjectImageRequest request = new UpdateProjectImageRequest("Alt", "Caption");
        when(projectService.updateProjectImage(eq(1L), eq(999L), any()))
            .thenThrow(new ResourceNotFoundException("ProjectImage", "id", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1/images/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(projectService).updateProjectImage(eq(1L), eq(999L), any());
    }

    // ========== Set Primary Image Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_setPrimaryImageWithValidIds() throws Exception {
        // Arrange
        doNothing().when(projectService).setPrimaryImage(1L, 10L);

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1/images/10/primary"))
            .andExpect(status().isOk());

        verify(projectService).setPrimaryImage(1L, 10L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_setPrimaryImageForNonexistentImage() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("ProjectImage", "id", 999L))
            .when(projectService).setPrimaryImage(1L, 999L);

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1/images/999/primary"))
            .andExpect(status().isNotFound());

        verify(projectService).setPrimaryImage(1L, 999L);
    }

    // ========== Reorder Images Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200_when_reorderImagesWithValidData() throws Exception {
        // Arrange
        ReorderProjectImagesRequest request = new ReorderProjectImagesRequest(Arrays.asList(3L, 1L, 2L));
        doNothing().when(projectService).reorderImages(eq(1L), any());

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/1/images/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(projectService).reorderImages(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_reorderImagesForNonexistentProject() throws Exception {
        // Arrange
        ReorderProjectImagesRequest request = new ReorderProjectImagesRequest(Arrays.asList(1L, 2L));
        doThrow(new ResourceNotFoundException("Project", "id", 999L))
            .when(projectService).reorderImages(eq(999L), any());

        // Act & Assert
        mockMvc.perform(put("/api/admin/projects/999/images/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(projectService).reorderImages(eq(999L), any());
    }
}
