package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * Unit tests for AdminTagController (admin endpoints).
 */
@WebMvcTest(AdminTagController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class AdminTagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    private TagResponse testTagResponse;
    private CreateTagRequest createRequest;
    private UpdateTagRequest updateRequest;

    @BeforeEach
    void setUp() {
        testTagResponse = new TagResponse(1L, "Java", "#FF5733");
        createRequest = new CreateTagRequest("Angular", "#DD0031");
        updateRequest = new UpdateTagRequest("Spring Boot", "#6DB33F");
    }

    // ========== Get All Tags Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndListOfTags_when_getAllTags() throws Exception {
        // Arrange
        TagResponse tag2 = new TagResponse(2L, "Spring", "#6DB33F");
        when(tagService.getAllTags()).thenReturn(List.of(testTagResponse, tag2));

        // Act / Assert
        mockMvc.perform(get("/api/admin/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Java")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Spring")));

        verify(tagService).getAllTags();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndEmptyList_when_getAllTagsWithNoTags() throws Exception {
        // Arrange
        when(tagService.getAllTags()).thenReturn(List.of());

        // Act / Assert
        mockMvc.perform(get("/api/admin/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(tagService).getAllTags();
    }

    // ========== Get Tag By ID Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndTag_when_getTagByIdWithExistingId() throws Exception {
        // Arrange
        when(tagService.getTagById(1L)).thenReturn(testTagResponse);

        // Act / Assert
        mockMvc.perform(get("/api/admin/tags/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Java")))
                .andExpect(jsonPath("$.color", is("#FF5733")));

        verify(tagService).getTagById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_getTagByIdWithNonExistingId() throws Exception {
        // Arrange
        when(tagService.getTagById(999L))
                .thenThrow(new ResourceNotFoundException("Tag not found with id: 999"));

        // Act / Assert
        mockMvc.perform(get("/api/admin/tags/999"))
                .andExpect(status().isNotFound());

        verify(tagService).getTagById(999L);
    }

    // ========== Create Tag Tests ==========

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return201AndCreatedTag_when_createTagWithValidRequest() throws Exception {
        when(tagService.createTag(any(CreateTagRequest.class))).thenReturn(testTagResponse);

        mockMvc.perform(post("/api/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Java")))
                .andExpect(jsonPath("$.color", is("#FF5733")));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return400_when_createTagWithInvalidRequest() throws Exception {
        CreateTagRequest invalidRequest = new CreateTagRequest("", "invalid-color");

        mockMvc.perform(post("/api/admin/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return200AndUpdatedTag_when_updateTagWithValidRequest() throws Exception {
        TagResponse updatedResponse = new TagResponse(1L, "Spring Boot", "#6DB33F");
        when(tagService.updateTag(eq(1L), any(UpdateTagRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/admin/tags/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Spring Boot")))
                .andExpect(jsonPath("$.color", is("#6DB33F")));

        verify(tagService).updateTag(eq(1L), any(UpdateTagRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_updateTagWithNonExistingId() throws Exception {
        when(tagService.updateTag(eq(999L), any(UpdateTagRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tag not found with id: 999"));

        mockMvc.perform(put("/api/admin/tags/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(tagService).updateTag(eq(999L), any(UpdateTagRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return204_when_deleteTagWithExistingId() throws Exception {
        mockMvc.perform(delete("/api/admin/tags/1"))
                .andExpect(status().isNoContent());

        verify(tagService).deleteTag(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void should_return404_when_deleteTagWithNonExistingId() throws Exception {
        doThrow(new ResourceNotFoundException("Tag not found with id: 999"))
                .when(tagService).deleteTag(999L);

        mockMvc.perform(delete("/api/admin/tags/999"))
                .andExpect(status().isNotFound());

        verify(tagService).deleteTag(999L);
    }
}
