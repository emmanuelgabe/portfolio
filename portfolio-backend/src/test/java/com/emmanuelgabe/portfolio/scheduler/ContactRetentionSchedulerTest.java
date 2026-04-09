package com.emmanuelgabe.portfolio.scheduler;

import com.emmanuelgabe.portfolio.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactRetentionSchedulerTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactRetentionScheduler scheduler;

    // ========== Purge Tests ==========

    @Test
    void should_deleteOldContacts_when_purgeExpiredContactsCalled() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "retentionMonths", 12);
        when(contactRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(5);

        // Act
        scheduler.purgeExpiredContacts();

        // Assert
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(contactRepository).deleteExpiredBefore(cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertThat(cutoff).isBefore(LocalDateTime.now().minusMonths(11));
        assertThat(cutoff).isAfter(LocalDateTime.now().minusMonths(13));
    }

    @Test
    void should_useConfiguredRetention_when_customRetentionSet() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "retentionMonths", 6);
        when(contactRepository.deleteExpiredBefore(any(LocalDateTime.class))).thenReturn(0);

        // Act
        scheduler.purgeExpiredContacts();

        // Assert
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(contactRepository).deleteExpiredBefore(cutoffCaptor.capture());
        LocalDateTime cutoff = cutoffCaptor.getValue();
        assertThat(cutoff).isBefore(LocalDateTime.now().minusMonths(5));
        assertThat(cutoff).isAfter(LocalDateTime.now().minusMonths(7));
    }

    @Test
    void should_notThrow_when_deleteExpiredBeforeFails() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "retentionMonths", 12);
        doThrow(new RuntimeException("DB error"))
                .when(contactRepository).deleteExpiredBefore(any(LocalDateTime.class));

        // Act & Assert
        assertThatCode(() -> scheduler.purgeExpiredContacts())
                .doesNotThrowAnyException();
    }
}
