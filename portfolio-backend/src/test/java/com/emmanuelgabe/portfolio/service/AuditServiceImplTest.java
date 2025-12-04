package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.audit.AuditAction;
import com.emmanuelgabe.portfolio.audit.AuditContext;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogFilter;
import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.entity.UserRole;
import com.emmanuelgabe.portfolio.mapper.AuditLogMapper;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import com.emmanuelgabe.portfolio.messaging.publisher.EventPublisher;
import com.emmanuelgabe.portfolio.repository.ArticleRepository;
import com.emmanuelgabe.portfolio.repository.AuditLogRepository;
import com.emmanuelgabe.portfolio.repository.CvRepository;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.repository.ProjectRepository;
import com.emmanuelgabe.portfolio.repository.SiteConfigurationRepository;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.repository.UserRepository;
import com.emmanuelgabe.portfolio.service.impl.AuditServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditServiceImpl.
 * Tests audit logging operations including create, stats, and export.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CvRepository cvRepository;

    @Mock
    private SiteConfigurationRepository siteConfigurationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @InjectMocks
    private AuditServiceImpl auditService;

    private User testUser;
    private AuditLog testAuditLog;
    private AuditLogResponse testAuditLogResponse;
    private AuditContext testContext;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setRole(UserRole.ROLE_ADMIN);

        testAuditLog = new AuditLog();
        testAuditLog.setId(1L);
        testAuditLog.setAction(AuditAction.CREATE);
        testAuditLog.setEntityType("Project");
        testAuditLog.setEntityId(1L);
        testAuditLog.setEntityName("Test Project");
        testAuditLog.setUsername("admin");
        testAuditLog.setSuccess(true);
        testAuditLog.setCreatedAt(LocalDateTime.now());

        testAuditLogResponse = new AuditLogResponse();
        testAuditLogResponse.setId(1L);
        testAuditLogResponse.setAction(AuditAction.CREATE);
        testAuditLogResponse.setEntityType("Project");
        testAuditLogResponse.setEntityId(1L);
        testAuditLogResponse.setEntityName("Test Project");
        testAuditLogResponse.setUsername("admin");
        testAuditLogResponse.setSuccess(true);

        testContext = AuditContext.builder()
                .userId(1L)
                .username("admin")
                .userRole("ADMIN")
                .ipAddress("127.0.0.1")
                .userAgent("Test Agent")
                .requestMethod("POST")
                .requestUri("/api/admin/projects")
                .requestId("test-request-id")
                .build();

        lenient().when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(testAuditLogResponse);
    }

    // ========== CreateAuditLog Tests ==========

    @Test
    void should_publishAuditEvent_when_createAuditLogCalledWithValidContext() {
        // Arrange
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("title", "Test Project");
        newValues.put("description", "Test Description");

        // Act
        AuditLog result = auditService.createAuditLog(
                testContext,
                AuditAction.CREATE,
                "Project",
                1L,
                "Test Project",
                null,
                newValues,
                true,
                null
        );

        // Assert
        assertThat(result).isNull(); // Async - no immediate result
        verify(eventPublisher).publishAuditEvent(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(event.getEntityType()).isEqualTo("Project");
        assertThat(event.getEntityId()).isEqualTo(1L);
        assertThat(event.getEntityName()).isEqualTo("Test Project");
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getUserId()).isEqualTo(1L);
    }

    @Test
    void should_publishEventWithNullContext_when_createAuditLogCalledWithNullContext() {
        // Act
        auditService.createAuditLog(
                null,
                AuditAction.CREATE,
                "Project",
                1L,
                "Test Project",
                null,
                null,
                true,
                null
        );

        // Assert
        verify(eventPublisher).publishAuditEvent(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getUsername()).isNull();
        assertThat(event.getUserId()).isNull();
    }

    @Test
    void should_calculateChangedFields_when_createAuditLogCalledWithOldAndNewValues() {
        // Arrange
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("title", "Old Title");
        oldValues.put("description", "Old Description");
        oldValues.put("status", "draft");

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("title", "New Title");
        newValues.put("description", "Old Description");
        newValues.put("status", "published");

        // Act
        auditService.createAuditLog(
                null,
                AuditAction.UPDATE,
                "Project",
                1L,
                "Test Project",
                oldValues,
                newValues,
                true,
                null
        );

        // Assert
        verify(eventPublisher).publishAuditEvent(eventCaptor.capture());
        List<String> changedFields = eventCaptor.getValue().getChangedFields();
        assertThat(changedFields).containsExactlyInAnyOrder("title", "status");
        assertThat(changedFields).doesNotContain("description");
    }

    // ========== LogAuthEvent Tests ==========

    @Test
    void should_publishAuthEvent_when_logAuthEventCalledWithLoginSuccess() {
        // Act
        auditService.logAuthEvent(
                AuditAction.LOGIN,
                "admin",
                "127.0.0.1",
                "Mozilla/5.0",
                true,
                null
        );

        // Assert
        verify(eventPublisher).publishAuditEvent(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(event.getUsername()).isEqualTo("admin");
        assertThat(event.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getAuditType()).isEqualTo(AuditEvent.AuditEventType.AUTH_EVENT);
    }

    @Test
    void should_publishAuthEventWithError_when_logAuthEventCalledWithLoginFailure() {
        // Act
        auditService.logAuthEvent(
                AuditAction.LOGIN_FAILED,
                "wronguser",
                "127.0.0.1",
                "Mozilla/5.0",
                false,
                "Invalid credentials"
        );

        // Assert
        verify(eventPublisher).publishAuditEvent(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditAction.LOGIN_FAILED);
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("Invalid credentials");
    }

    // ========== GetAuditLogs Tests ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_returnPageOfAuditLogs_when_getAuditLogsCalledWithFilter() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        filter.setAction(AuditAction.CREATE);
        Pageable pageable = PageRequest.of(0, 20);

        List<AuditLog> logs = List.of(testAuditLog);
        Page<AuditLog> page = new PageImpl<>(logs, pageable, 1);

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = auditService.getAuditLogs(filter, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnEmptyPage_when_getAuditLogsCalledWithNoResults() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        // Act
        Page<AuditLogResponse> result = auditService.getAuditLogs(filter, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
    }

    // ========== GetEntityHistory Tests ==========

    @Test
    void should_returnEntityHistory_when_getEntityHistoryCalledWithValidEntity() {
        // Arrange
        List<AuditLog> history = List.of(testAuditLog);
        when(auditLogRepository.findEntityHistory("Project", 1L)).thenReturn(history);

        // Act
        List<AuditLogResponse> result = auditService.getEntityHistory("Project", 1L);

        // Assert
        assertThat(result).hasSize(1);
        verify(auditLogRepository).findEntityHistory("Project", 1L);
    }

    @Test
    void should_returnEmptyList_when_getEntityHistoryCalledWithNoHistory() {
        // Arrange
        when(auditLogRepository.findEntityHistory("Project", 999L)).thenReturn(List.of());

        // Act
        List<AuditLogResponse> result = auditService.getEntityHistory("Project", 999L);

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== GetStats Tests ==========

    @Test
    void should_returnStats_when_getStatsCalledWithValidDate() {
        // Arrange
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        when(auditLogRepository.countByActionSince(since)).thenReturn(List.of(
                new Object[]{AuditAction.CREATE, 10L},
                new Object[]{AuditAction.UPDATE, 5L}
        ));
        when(auditLogRepository.countByEntityTypeSince(since)).thenReturn(List.of(
                new Object[]{"Project", 8L},
                new Object[]{"Article", 7L}
        ));
        List<Object[]> dateStats = new java.util.ArrayList<>();
        dateStats.add(new Object[]{java.sql.Date.valueOf("2024-01-01"), 3L});
        when(auditLogRepository.countByDateSince(since)).thenReturn(dateStats);

        List<Object[]> userStats = new java.util.ArrayList<>();
        userStats.add(new Object[]{"admin", 15L});
        when(auditLogRepository.countByUserSince(since)).thenReturn(userStats);
        when(auditLogRepository.countTotalAndFailedSince(since)).thenReturn(new Object[]{15L, 2L});

        // Act
        AuditStatsResponse result = auditService.getStats(since);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getActionCounts()).containsEntry("CREATE", 10L);
        assertThat(result.getActionCounts()).containsEntry("UPDATE", 5L);
        assertThat(result.getEntityTypeCounts()).containsEntry("Project", 8L);
        assertThat(result.getFailedActionsCount()).isEqualTo(2L);
        assertThat(result.getTotalCount()).isEqualTo(15L);
    }

    // ========== ExportToCsv Tests ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_returnCsvBytes_when_exportToCsvCalled() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        testAuditLog.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        Page<AuditLog> page = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Act
        byte[] result = auditService.exportToCsv(filter);

        // Assert
        assertThat(result).isNotNull();
        String csv = new String(result);
        assertThat(csv).contains("ID,Timestamp,Action,Entity Type");
        assertThat(csv).contains("CREATE");
        assertThat(csv).contains("Project");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnEmptyCsv_when_exportToCsvCalledWithNoData() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        byte[] result = auditService.exportToCsv(filter);

        // Assert
        assertThat(result).isNotNull();
        String csv = new String(result);
        assertThat(csv).contains("ID,Timestamp,Action,Entity Type");
    }

    // ========== ExportToJson Tests ==========

    @Test
    @SuppressWarnings("unchecked")
    void should_returnJsonBytes_when_exportToJsonCalled() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        Page<AuditLog> page = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Act
        byte[] result = auditService.exportToJson(filter);

        // Assert
        assertThat(result).isNotNull();
        String json = new String(result);
        assertThat(json).contains("\"action\"");
        assertThat(json).contains("\"entityType\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_returnEmptyJsonArray_when_exportToJsonCalledWithNoData() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        byte[] result = auditService.exportToJson(filter);

        // Assert
        assertThat(result).isNotNull();
        String json = new String(result);
        assertThat(json.trim()).isEqualTo("[ ]");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_escapeCsvSpecialCharacters_when_exportToCsvCalledWithSpecialChars() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        testAuditLog.setEntityName("Test, \"Project\" with\nnewline");
        testAuditLog.setErrorMessage("Error with, comma");
        testAuditLog.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        Page<AuditLog> page = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Act
        byte[] result = auditService.exportToCsv(filter);

        // Assert
        assertThat(result).isNotNull();
        String csv = new String(result);
        assertThat(csv).contains("\"Test, \"\"Project\"\" with");
        assertThat(csv).contains("\"Error with, comma\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_handleNullFields_when_exportToCsvCalledWithNullValues() {
        // Arrange
        AuditLogFilter filter = new AuditLogFilter();
        testAuditLog.setEntityId(null);
        testAuditLog.setEntityName(null);
        testAuditLog.setIpAddress(null);
        testAuditLog.setErrorMessage(null);
        testAuditLog.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));

        Page<AuditLog> page = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Act
        byte[] result = auditService.exportToCsv(filter);

        // Assert
        assertThat(result).isNotNull();
        String csv = new String(result);
        assertThat(csv).contains("CREATE");
        assertThat(csv).contains("Project");
    }

    // ========== ConvertToMap Tests ==========

    @Test
    void should_returnMap_when_convertToMapCalledWithValidObject() {
        // Arrange
        Map<String, Object> testObject = new HashMap<>();
        testObject.put("key1", "value1");
        testObject.put("key2", 123);

        // Act
        Map<String, Object> result = auditService.convertToMap(testObject);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("key1", "value1");
        assertThat(result).containsEntry("key2", 123);
    }

    @Test
    void should_returnNull_when_convertToMapCalledWithNull() {
        // Act
        Map<String, Object> result = auditService.convertToMap(null);

        // Assert
        assertThat(result).isNull();
    }
}
