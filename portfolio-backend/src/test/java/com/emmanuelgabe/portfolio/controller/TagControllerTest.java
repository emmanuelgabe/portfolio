package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
 * Unit tests for TagController (public endpoints only).
 * Admin endpoint tests are in AdminTagControllerTest.
 */
@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TagService tagService;

    private TagResponse testTagResponse;

    @BeforeEach
    void setUp() {
        testTagResponse = new TagResponse(1L, "Java", "#FF5733");
    }

    @Test
    void should_return200AndListOfTags_when_getAllTags() throws Exception {
        List<TagResponse> tags = List.of(
                testTagResponse,
                new TagResponse(2L, "TypeScript", "#3178C6")
        );
        when(tagService.getAllTags()).thenReturn(tags);

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Java")))
                .andExpect(jsonPath("$[0].color", is("#FF5733")));

        verify(tagService).getAllTags();
    }

    @Test
    void should_return200AndTag_when_getTagByIdWithExistingId() throws Exception {
        when(tagService.getTagById(1L)).thenReturn(testTagResponse);

        mockMvc.perform(get("/api/tags/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Java")))
                .andExpect(jsonPath("$.color", is("#FF5733")));

        verify(tagService).getTagById(1L);
    }

    @Test
    void should_return404_when_getTagByIdWithNonExistingId() throws Exception {
        when(tagService.getTagById(999L))
                .thenThrow(new ResourceNotFoundException("Tag not found with id: 999"));

        mockMvc.perform(get("/api/tags/999"))
                .andExpect(status().isNotFound());

        verify(tagService).getTagById(999L);
    }
}
