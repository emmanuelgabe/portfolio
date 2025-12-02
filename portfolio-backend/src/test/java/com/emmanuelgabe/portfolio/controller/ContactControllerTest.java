package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.config.TestSecurityConfig;
import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.service.ContactService;
import com.emmanuelgabe.portfolio.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactService contactService;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    void should_sendContactMessage_when_validRequestAndNotRateLimited() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        ContactResponse response = ContactResponse.success("Message sent successfully");

        when(rateLimitService.isAllowed(anyString())).thenReturn(true);
        when(contactService.sendContactEmail(any(ContactRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message sent successfully"));

        verify(rateLimitService).isAllowed(anyString());
        verify(contactService).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_returnTooManyRequests_when_rateLimitExceeded() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        when(rateLimitService.isAllowed(anyString())).thenReturn(false);
        when(rateLimitService.getRemainingAttempts(anyString())).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(rateLimitService).isAllowed(anyString());
        verify(rateLimitService).getRemainingAttempts(anyString());
        verify(contactService, never()).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_returnBadRequest_when_nameIsBlank() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimitService, never()).isAllowed(anyString());
        verify(contactService, never()).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_returnBadRequest_when_emailIsInvalid() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "invalid-email",
                "Test Subject",
                "This is a test message"
        );

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimitService, never()).isAllowed(anyString());
        verify(contactService, never()).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_returnBadRequest_when_subjectIsTooShort() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Hi",
                "This is a test message"
        );

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimitService, never()).isAllowed(anyString());
        verify(contactService, never()).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_returnBadRequest_when_messageIsTooShort() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "Short"
        );

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(rateLimitService, never()).isAllowed(anyString());
        verify(contactService, never()).sendContactEmail(any(ContactRequest.class));
    }

    @Test
    void should_extractIpFromXForwardedForHeader_when_present() throws Exception {
        // Arrange
        ContactRequest request = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Test Subject",
                "This is a test message"
        );

        ContactResponse response = ContactResponse.success("Message sent successfully");

        when(rateLimitService.isAllowed("192.168.1.1")).thenReturn(true);
        when(contactService.sendContactEmail(any(ContactRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(rateLimitService).isAllowed("192.168.1.1");
    }
}
