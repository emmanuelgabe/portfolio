package com.emmanuelgabe.portfolio.service.impl;

import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.dto.ContactStatsResponse;
import com.emmanuelgabe.portfolio.entity.Contact;
import com.emmanuelgabe.portfolio.mapper.ContactMapper;
import com.emmanuelgabe.portfolio.messaging.event.AutoReplyEvent;
import com.emmanuelgabe.portfolio.messaging.event.ContactEmailEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.metrics.BusinessMetrics;
import com.emmanuelgabe.portfolio.moderation.ContactModerationService;
import com.emmanuelgabe.portfolio.moderation.ModerationResult;
import com.emmanuelgabe.portfolio.moderation.ModerationVerdict;
import com.emmanuelgabe.portfolio.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactServiceImpl}.
 * Verifies the contact flow orchestration: moderation gate, persistence,
 * email event publication, auto-reply event publication, and admin queries.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    private static final String RECIPIENT_EMAIL = "owner@portfolio.com";

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ContactModerationService moderationService;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private ContactServiceImpl contactService;

    private ContactRequest validRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientEmail", RECIPIENT_EMAIL);
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", true);

        validRequest = new ContactRequest(
                "John Doe",
                "john@example.com",
                "Project Inquiry",
                "I would like to discuss a collaboration opportunity"
        );
    }

    // ========== Approved Flow Tests ==========

    @Test
    void should_publishEmailAndAutoReplyEvents_when_messageApprovedAndAutoReplyEnabled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_publishOnlyEmailEvent_when_autoReplyDisabled() {
        // Arrange
        ReflectionTestUtils.setField(contactService, "autoReplyEnabled", false);
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher, never()).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_useConfiguredRecipientEmail_when_buildingEmailEvent() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<ContactEmailEvent> eventCaptor = ArgumentCaptor.forClass(ContactEmailEvent.class);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());
        ContactEmailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRecipientEmail()).isEqualTo(RECIPIENT_EMAIL);
        assertThat(capturedEvent.getSenderEmail()).isEqualTo(validRequest.getEmail());
        assertThat(capturedEvent.getSenderName()).isEqualTo(validRequest.getName());
        assertThat(capturedEvent.getSubject()).isEqualTo(validRequest.getSubject());
    }

    @Test
    void should_useConfiguredRecipientEmail_when_buildingAutoReplyEvent() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<AutoReplyEvent> eventCaptor = ArgumentCaptor.forClass(AutoReplyEvent.class);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishAutoReplyEvent(eventCaptor.capture());
        AutoReplyEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getRecipientEmail()).isEqualTo(RECIPIENT_EMAIL);
        assertThat(capturedEvent.getSenderEmail()).isEqualTo(validRequest.getEmail());
        assertThat(capturedEvent.getSenderName()).isEqualTo(validRequest.getName());
        assertThat(capturedEvent.getSubject()).isEqualTo(validRequest.getSubject());
        assertThat(capturedEvent.getMessage()).isEqualTo(validRequest.getMessage());
    }

    @Test
    void should_escapeHtmlSpecialCharacters_when_buildingEmailBody() {
        // Arrange
        ContactRequest maliciousRequest = new ContactRequest(
                "<script>alert('xss')</script>",
                "attacker@example.com",
                "Subject with \"quotes\" & <tags>",
                "Message with <b>html</b> and 'quotes' & ampersand"
        );
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        ArgumentCaptor<ContactEmailEvent> eventCaptor = ArgumentCaptor.forClass(ContactEmailEvent.class);

        // Act
        contactService.sendContactEmail(maliciousRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(eventCaptor.capture());
        String body = eventCaptor.getValue().getBody();
        assertThat(body).doesNotContain("<script>");
        assertThat(body).contains("&lt;script&gt;");
        assertThat(body).contains("&amp;");
        assertThat(body).contains("&quot;");
        assertThat(body).contains("&#x27;");
    }

    // ========== Rejected Flow Tests ==========

    @Test
    void should_notPublishAnyEvent_when_messageRejected() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam detected", 0.95));

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher, never()).publishEmailEvent(any(ContactEmailEvent.class));
        verify(eventPublisher, never()).publishAutoReplyEvent(any(AutoReplyEvent.class));
    }

    @Test
    void should_returnGenericSuccess_when_messageRejected() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.rejected("Spam detected", 0.95));

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).doesNotContainIgnoringCase("spam");
        assertThat(response.getMessage()).doesNotContainIgnoringCase("rejected");
        assertThat(response.getMessage()).doesNotContainIgnoringCase("blocked");
    }

    // ========== Persistence Tests ==========

    @Test
    void should_persistContact_when_messageApproved() {
        // Arrange
        ModerationResult moderation = new ModerationResult(
                ModerationVerdict.APPROVED, ContactCategory.MISSION, "Job offer", 0.95);
        when(moderationService.moderate(any(ContactRequest.class))).thenReturn(moderation);
        ArgumentCaptor<Contact> contactCaptor = ArgumentCaptor.forClass(Contact.class);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(contactRepository).save(contactCaptor.capture());
        Contact saved = contactCaptor.getValue();
        assertThat(saved.getName()).isEqualTo(validRequest.getName());
        assertThat(saved.getSubject()).isEqualTo(validRequest.getSubject());
        assertThat(saved.getMessage()).isEqualTo(validRequest.getMessage());
        assertThat(saved.getCategory()).isEqualTo(ContactCategory.MISSION);
        assertThat(saved.getModerationVerdict()).isEqualTo(ModerationVerdict.APPROVED);
        assertThat(saved.getModerationReason()).isEqualTo("Job offer");
        assertThat(saved.getConfidenceScore()).isEqualTo(0.95);
    }

    @Test
    void should_persistContact_when_messageRejected() {
        // Arrange
        ModerationResult moderation = new ModerationResult(
                ModerationVerdict.REJECTED, ContactCategory.SPAM, "SEO spam", 0.92);
        when(moderationService.moderate(any(ContactRequest.class))).thenReturn(moderation);

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void should_continueFlowAndRecordMetric_when_persistenceFails() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        doThrow(new RuntimeException("DB unavailable"))
                .when(contactRepository).save(any(Contact.class));

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
        verify(metrics).recordContactPersistenceFailure();
    }

    // ========== Fail-Open Tests ==========

    @Test
    void should_returnSuccess_when_autoReplyPublicationFails() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));

        // Act
        ContactResponse response = contactService.sendContactEmail(validRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void should_stillPublishEmailEvent_when_autoReplyPublicationFails() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(eventPublisher).publishAutoReplyEvent(any(AutoReplyEvent.class));

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }

    // ========== Moderation Interaction Tests ==========

    @Test
    void should_callModerationService_when_sendContactEmailCalled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert
        verify(moderationService).moderate(validRequest);
    }

    @Test
    void should_callModerationBeforeEmailPublication_when_sendContactEmailCalled() {
        // Arrange
        when(moderationService.moderate(any(ContactRequest.class)))
                .thenReturn(ModerationResult.approved());

        // Act
        contactService.sendContactEmail(validRequest);

        // Assert -- verify ordering via InOrder
        var inOrder = org.mockito.Mockito.inOrder(moderationService, eventPublisher);
        inOrder.verify(moderationService).moderate(any(ContactRequest.class));
        inOrder.verify(eventPublisher).publishEmailEvent(any(ContactEmailEvent.class));
    }

    // ========== Admin Stats Tests ==========

    @Test
    void should_returnStats_when_getContactStatsCalled() {
        // Arrange
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        when(contactRepository.countSince(since)).thenReturn(10L);
        when(contactRepository.countByCategorySince(since)).thenReturn(List.of(
                new Object[]{ContactCategory.MISSION, 5L},
                new Object[]{ContactCategory.SPAM, 3L}
        ));
        when(contactRepository.countByVerdictSince(since)).thenReturn(List.of(
                new Object[]{ModerationVerdict.APPROVED, 7L},
                new Object[]{ModerationVerdict.REJECTED, 3L}
        ));
        when(contactRepository.countDailySince(since)).thenReturn(List.of(
                new Object[]{"2026-04-06", 4L},
                new Object[]{"2026-04-07", 6L}
        ));

        // Act
        ContactStatsResponse stats = contactService.getContactStats(since);

        // Assert
        assertThat(stats.getTotalCount()).isEqualTo(10);
        assertThat(stats.getCategoryCounts()).containsEntry("MISSION", 5L);
        assertThat(stats.getCategoryCounts()).containsEntry("SPAM", 3L);
        assertThat(stats.getVerdictCounts()).containsEntry("APPROVED", 7L);
        assertThat(stats.getDailyActivity()).hasSize(2);
    }

    // ========== Admin History Tests ==========

    @Test
    void should_returnAllContacts_when_getContactHistoryCalledWithoutFilter() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setName("John Doe");
        Page<Contact> page = new PageImpl<>(List.of(contact));
        when(contactRepository.findAll(pageable)).thenReturn(page);
        when(contactMapper.toHistoryResponse(contact))
                .thenReturn(new ContactHistoryResponse(
                        1L, "John Doe", "Subject", "Message",
                        ContactCategory.MISSION, ModerationVerdict.APPROVED,
                        "Job offer", 0.95, LocalDateTime.now()));

        // Act
        Page<ContactHistoryResponse> result = contactService.getContactHistory(null, pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("John Doe");
    }

    @Test
    void should_filterByCategory_when_getContactHistoryCalledWithCategory() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contact> emptyPage = new PageImpl<>(List.of());
        when(contactRepository.findByCategory(ContactCategory.SPAM, pageable)).thenReturn(emptyPage);

        // Act
        Page<ContactHistoryResponse> result = contactService.getContactHistory(
                ContactCategory.SPAM, pageable);

        // Assert
        assertThat(result.getTotalElements()).isZero();
        verify(contactRepository).findByCategory(ContactCategory.SPAM, pageable);
        verify(contactRepository, never()).findAll(pageable);
    }
}
