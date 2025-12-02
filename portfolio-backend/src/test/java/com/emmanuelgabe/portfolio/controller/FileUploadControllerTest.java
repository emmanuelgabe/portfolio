package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.FileUploadResponse;
import com.emmanuelgabe.portfolio.mapper.FileUploadMapper;
import com.emmanuelgabe.portfolio.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private FileUploadMapper fileUploadMapper;

    @Test
    void should_uploadImage_when_validFileProvided() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        String expectedFileName = "test-image.jpg";
        String expectedFileUrl = "http://localhost:8080/uploads/test-image.jpg";
        FileUploadResponse expectedResponse = new FileUploadResponse(
                expectedFileName,
                expectedFileUrl,
                MediaType.IMAGE_JPEG_VALUE,
                file.getSize()
        );

        when(fileStorageService.storeFile(any())).thenReturn(expectedFileName);
        when(fileStorageService.getFileUrl(expectedFileName)).thenReturn(expectedFileUrl);
        when(fileUploadMapper.toResponse(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/upload/image")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value(expectedFileName))
                .andExpect(jsonPath("$.fileUrl").value(expectedFileUrl))
                .andExpect(jsonPath("$.fileType").value(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(jsonPath("$.fileSize").value(file.getSize()));
    }
}
