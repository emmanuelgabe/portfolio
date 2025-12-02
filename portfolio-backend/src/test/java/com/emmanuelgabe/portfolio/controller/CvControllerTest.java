package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.CvService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class CvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CvService cvService;

    private CvResponse testCvResponse;
    private MockMultipartFile validPdfFile;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@test.com");
        testUser.setRole(UserRole.ROLE_ADMIN);

        // Setup Security Context with test user
        setupSecurityContext(testUser);
        testCvResponse = new CvResponse();
        testCvResponse.setId(1L);
        testCvResponse.setFileName("cv_20240101_120000_abc123.pdf");
        testCvResponse.setOriginalFileName("my_cv.pdf");
        testCvResponse.setFileUrl("/uploads/cvs/cv_20240101_120000_abc123.pdf");
        testCvResponse.setFileSize(1024L);
        testCvResponse.setUploadedAt(LocalDateTime.now());
        testCvResponse.setCurrent(true);

        // Create valid PDF file (with PDF magic bytes)
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        validPdfFile = new MockMultipartFile(
                "file",
                "test_cv.pdf",
                "application/pdf",
                pdfContent
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return201AndCvResponse_when_uploadCvCalledWithValidPdf() throws Exception {
        // Arrange
        when(cvService.uploadCv(any(), any(User.class))).thenReturn(testCvResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/cv/upload")
                        .file(validPdfFile))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fileName", is("cv_20240101_120000_abc123.pdf")))
                .andExpect(jsonPath("$.originalFileName", is("my_cv.pdf")))
                .andExpect(jsonPath("$.fileUrl", is("/uploads/cvs/cv_20240101_120000_abc123.pdf")))
                .andExpect(jsonPath("$.fileSize", is(1024)))
                .andExpect(jsonPath("$.current", is(true)));

        verify(cvService).uploadCv(any(), any(User.class));
    }

    @Test
    void should_return200AndCvResponse_when_getCurrentCvCalledWithExistingCv() throws Exception {
        // Arrange
        when(cvService.getCurrentCv()).thenReturn(Optional.of(testCvResponse));

        // Act & Assert
        mockMvc.perform(get("/api/cv/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fileName", is("cv_20240101_120000_abc123.pdf")))
                .andExpect(jsonPath("$.current", is(true)));

        verify(cvService).getCurrentCv();
    }

    @Test
    void should_return204NoContent_when_getCurrentCvCalledWithNoCv() throws Exception {
        // Arrange
        when(cvService.getCurrentCv()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/cv/current"))
                .andExpect(status().isNoContent());

        verify(cvService).getCurrentCv();
    }

    @Test
    void should_return200AndPdfFile_when_downloadCvCalledWithExistingCv() throws Exception {
        // Arrange
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
        Resource resource = new ByteArrayResource(pdfContent);
        when(cvService.downloadCurrentCv()).thenReturn(resource);

        // Act & Assert
        mockMvc.perform(get("/api/cv/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"emmanuel_gabe_cv_developpeur_backend.pdf\""))
                .andExpect(content().bytes(pdfContent));

        verify(cvService).downloadCurrentCv();
    }

    @Test
    void should_return404_when_downloadCvCalledWithNoCv() throws Exception {
        // Arrange
        when(cvService.downloadCurrentCv())
                .thenThrow(new ResourceNotFoundException("No current CV found"));

        // Act & Assert
        mockMvc.perform(get("/api/cv/download"))
                .andExpect(status().isNotFound());

        verify(cvService).downloadCurrentCv();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return200AndListOfCvs_when_getAllCvsCalled() throws Exception {
        // Arrange
        CvResponse cv1 = new CvResponse();
        cv1.setId(1L);
        cv1.setCurrent(true);

        CvResponse cv2 = new CvResponse();
        cv2.setId(2L);
        cv2.setCurrent(false);

        List<CvResponse> cvs = Arrays.asList(cv1, cv2);
        when(cvService.getAllCvs(any())).thenReturn(cvs);

        // Act & Assert
        mockMvc.perform(get("/api/cv/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].current", is(true)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].current", is(false)));

        verify(cvService).getAllCvs(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return200AndUpdatedCv_when_setCurrentCvCalled() throws Exception {
        // Arrange
        when(cvService.setCurrentCv(eq(1L), any(User.class))).thenReturn(testCvResponse);

        // Act & Assert
        mockMvc.perform(put("/api/cv/1/set-current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.current", is(true)));

        verify(cvService).setCurrentCv(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return204_when_deleteCvCalledSuccessfully() throws Exception {
        // Arrange
        doNothing().when(cvService).deleteCv(eq(1L), any(User.class));

        // Act & Assert
        mockMvc.perform(delete("/api/cv/1"))
                .andExpect(status().isNoContent());

        verify(cvService).deleteCv(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_return404_when_deleteCvCalledWithNonExistentCv() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("CV not found"))
                .when(cvService).deleteCv(eq(999L), any(User.class));

        // Act & Assert
        mockMvc.perform(delete("/api/cv/999"))
                .andExpect(status().isNotFound());

        verify(cvService).deleteCv(eq(999L), any(User.class));
    }

    private void setupSecurityContext(User user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
