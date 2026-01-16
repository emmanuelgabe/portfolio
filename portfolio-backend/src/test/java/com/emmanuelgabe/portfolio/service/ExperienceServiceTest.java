package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.entity.Experience;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.ExperienceMapper;
import com.emmanuelgabe.portfolio.repository.ExperienceRepository;
import com.emmanuelgabe.portfolio.service.impl.ExperienceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private ExperienceMapper experienceMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    private Experience testExperience;
    private ExperienceResponse testExperienceResponse;

    @BeforeEach
    void setUp() {
        testExperience = new Experience();
        testExperience.setId(1L);
        testExperience.setCompany("Test Company");
        testExperience.setRole("Software Engineer");
        testExperience.setStartDate(LocalDate.of(2022, 1, 1));
        testExperience.setEndDate(LocalDate.of(2023, 12, 31));
        testExperience.setDescription("Test description for experience");
        testExperience.setType(ExperienceType.WORK);
        testExperience.setShowMonths(true);
        testExperience.setCreatedAt(LocalDateTime.now());

        testExperience.setUpdatedAt(LocalDateTime.now());

        testExperienceResponse = new ExperienceResponse();
        testExperienceResponse.setId(1L);
        testExperienceResponse.setCompany("Test Company");
        testExperienceResponse.setRole("Software Engineer");
        testExperienceResponse.setStartDate(LocalDate.of(2022, 1, 1));
        testExperienceResponse.setEndDate(LocalDate.of(2023, 12, 31));
        testExperienceResponse.setDescription("Test description for experience");
        testExperienceResponse.setType(ExperienceType.WORK);
        testExperienceResponse.setShowMonths(true);
        testExperienceResponse.setCreatedAt(testExperience.getCreatedAt());

        testExperienceResponse.setUpdatedAt(testExperience.getUpdatedAt());
        testExperienceResponse.setOngoing(false);

        // Mock mapper behavior
        lenient().when(experienceMapper.toResponse(any(Experience.class))).thenAnswer(invocation -> {
            Experience experience = invocation.getArgument(0);
            ExperienceResponse response = new ExperienceResponse();
            response.setId(experience.getId());
            response.setCompany(experience.getCompany());
            response.setRole(experience.getRole());
            response.setStartDate(experience.getStartDate());
            response.setEndDate(experience.getEndDate());
            response.setDescription(experience.getDescription());
            response.setType(experience.getType());
            response.setShowMonths(experience.isShowMonths());
            response.setCreatedAt(experience.getCreatedAt());

            response.setUpdatedAt(experience.getUpdatedAt());
            response.setOngoing(experience.isOngoing());
            return response;
        });
        lenient().when(experienceMapper.toEntity(any(CreateExperienceRequest.class))).thenReturn(testExperience);
    }

    @Test
    void should_returnListOfExperiences_when_getAllExperiencesCalled() {
        // Arrange
        List<Experience> experiences = Arrays.asList(testExperience);
        when(experienceRepository.findAllByOrderByDisplayOrderAscStartDateDesc()).thenReturn(experiences);

        // Act
        List<ExperienceResponse> result = experienceService.getAllExperiences();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompany()).isEqualTo("Test Company");
        verify(experienceRepository).findAllByOrderByDisplayOrderAscStartDateDesc();
    }

    @Test
    void should_returnExperience_when_getExperienceByIdWithValidId() {
        // Arrange
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(testExperience));

        // Act
        ExperienceResponse result = experienceService.getExperienceById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCompany()).isEqualTo("Test Company");
        assertThat(result.getRole()).isEqualTo("Software Engineer");
        verify(experienceRepository).findById(1L);
    }

    @Test
    void should_throwResourceNotFoundException_when_getExperienceByIdWithInvalidId() {
        // Arrange
        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.getExperienceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience")
                .hasMessageContaining("999");
        verify(experienceRepository).findById(999L);
    }

    @Test
    void should_createExperience_when_validRequest() {
        // Arrange
        CreateExperienceRequest request = new CreateExperienceRequest();
        request.setCompany("New Company");
        request.setRole("Senior Developer");
        request.setStartDate(LocalDate.of(2023, 1, 1));
        request.setEndDate(null);
        request.setDescription("New experience description");
        request.setType(ExperienceType.WORK);
        request.setShowMonths(true);


        Experience newExperience = new Experience();
        newExperience.setId(2L);
        newExperience.setCompany("New Company");
        newExperience.setRole("Senior Developer");
        newExperience.setStartDate(LocalDate.of(2023, 1, 1));
        newExperience.setEndDate(null);
        newExperience.setDescription("New experience description");
        newExperience.setType(ExperienceType.WORK);
        newExperience.setShowMonths(true);
        newExperience.setCreatedAt(LocalDateTime.now());

        newExperience.setUpdatedAt(LocalDateTime.now());

        when(experienceMapper.toEntity(request)).thenReturn(newExperience);
        when(experienceRepository.save(any(Experience.class))).thenReturn(newExperience);

        // Act
        ExperienceResponse result = experienceService.createExperience(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompany()).isEqualTo("New Company");
        assertThat(result.getRole()).isEqualTo("Senior Developer");
        assertThat(result.isOngoing()).isTrue();
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    void should_throwIllegalStateException_when_createExperienceWithInvalidDates() {
        // Arrange
        CreateExperienceRequest request = new CreateExperienceRequest();
        request.setCompany("New Company");
        request.setRole("Senior Developer");
        request.setStartDate(LocalDate.of(2023, 12, 31));
        request.setEndDate(LocalDate.of(2023, 1, 1)); // End before start
        request.setDescription("Invalid experience");
        request.setType(ExperienceType.WORK);
        request.setShowMonths(true);


        Experience invalidExperience = new Experience();
        invalidExperience.setCompany("New Company");
        invalidExperience.setRole("Senior Developer");
        invalidExperience.setStartDate(LocalDate.of(2023, 12, 31));
        invalidExperience.setEndDate(LocalDate.of(2023, 1, 1));
        invalidExperience.setDescription("Invalid experience");
        invalidExperience.setType(ExperienceType.WORK);
        invalidExperience.setShowMonths(true);


        when(experienceMapper.toEntity(request)).thenReturn(invalidExperience);

        // Act & Assert
        assertThatThrownBy(() -> experienceService.createExperience(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("End date cannot be before start date");
        verify(experienceRepository, never()).save(any(Experience.class));
    }

    @Test
    void should_updateExperience_when_validRequest() {
        // Arrange
        UpdateExperienceRequest request = new UpdateExperienceRequest();
        request.setCompany("Updated Company");
        request.setRole("Lead Developer");

        when(experienceRepository.findById(1L)).thenReturn(Optional.of(testExperience));
        when(experienceRepository.save(any(Experience.class))).thenReturn(testExperience);

        // Act
        ExperienceResponse result = experienceService.updateExperience(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(experienceRepository).findById(1L);
        verify(experienceMapper).updateEntityFromRequest(request, testExperience);
        verify(experienceRepository).save(testExperience);
    }

    @Test
    void should_throwResourceNotFoundException_when_updateExperienceWithInvalidId() {
        // Arrange
        UpdateExperienceRequest request = new UpdateExperienceRequest();
        request.setCompany("Updated Company");

        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.updateExperience(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience")
                .hasMessageContaining("999");
        verify(experienceRepository).findById(999L);
        verify(experienceRepository, never()).save(any(Experience.class));
    }

    @Test
    void should_deleteExperience_when_validId() {
        // Arrange
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(testExperience));

        // Act
        experienceService.deleteExperience(1L);

        // Assert
        verify(experienceRepository).findById(1L);
        verify(experienceRepository).deleteById(1L);
    }

    @Test
    void should_throwResourceNotFoundException_when_deleteExperienceWithInvalidId() {
        // Arrange
        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.deleteExperience(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience")
                .hasMessageContaining("999");
        verify(experienceRepository).findById(999L);
        verify(experienceRepository, never()).deleteById(999L);
    }

    // ========== Reorder Tests ==========

    @Test
    void should_reorderExperiences_when_reorderExperiencesCalledWithValidRequest() {
        // Arrange
        Experience experience2 = new Experience();
        experience2.setId(2L);
        experience2.setCompany("Company 2");
        experience2.setRole("Role 2");
        experience2.setDescription("Description 2");

        ReorderRequest request = new ReorderRequest();
        request.setOrderedIds(Arrays.asList(2L, 1L));

        when(experienceRepository.findById(2L)).thenReturn(Optional.of(experience2));
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(testExperience));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        experienceService.reorderExperiences(request);

        // Assert
        verify(experienceRepository).findById(2L);
        verify(experienceRepository).findById(1L);
        verify(experienceRepository).save(experience2);
        verify(experienceRepository).save(testExperience);
        assertThat(experience2.getDisplayOrder()).isEqualTo(0);
        assertThat(testExperience.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void should_throwResourceNotFoundException_when_reorderExperiencesCalledWithInvalidId() {
        // Arrange
        ReorderRequest request = new ReorderRequest();
        request.setOrderedIds(Arrays.asList(1L, 999L));

        when(experienceRepository.findById(1L)).thenReturn(Optional.of(testExperience));
        when(experienceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> experienceService.reorderExperiences(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Experience")
                .hasMessageContaining("999");
    }

    // ========== Get By Type Tests ==========

    @Test
    void should_returnExperiencesByType_when_validType() {
        // Arrange
        when(experienceRepository.findByTypeOrderByDisplayOrderAscStartDateDesc(ExperienceType.WORK))
                .thenReturn(Arrays.asList(testExperience));

        // Act
        List<ExperienceResponse> result = experienceService.getExperiencesByType(ExperienceType.WORK);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(ExperienceType.WORK);
        verify(experienceRepository).findByTypeOrderByDisplayOrderAscStartDateDesc(ExperienceType.WORK);
    }

    @Test
    void should_returnOngoingExperiences_when_called() {
        // Arrange
        Experience ongoingExperience = new Experience();
        ongoingExperience.setId(2L);
        ongoingExperience.setCompany("Current Company");
        ongoingExperience.setRole("Current Role");
        ongoingExperience.setStartDate(LocalDate.of(2023, 1, 1));
        ongoingExperience.setEndDate(null);
        ongoingExperience.setDescription("Ongoing experience");
        ongoingExperience.setType(ExperienceType.WORK);

        when(experienceRepository.findByEndDateIsNullOrderByDisplayOrderAscStartDateDesc())
                .thenReturn(Arrays.asList(ongoingExperience));

        // Act
        List<ExperienceResponse> result = experienceService.getOngoingExperiences();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOngoing()).isTrue();
        verify(experienceRepository).findByEndDateIsNullOrderByDisplayOrderAscStartDateDesc();
    }

    @Test
    void should_returnRecentExperiences_when_limitSpecified() {
        // Arrange
        Experience experience2 = new Experience();
        experience2.setId(2L);
        experience2.setCompany("Company 2");
        experience2.setRole("Role 2");
        experience2.setStartDate(LocalDate.of(2023, 1, 1));
        experience2.setEndDate(LocalDate.of(2023, 12, 31));
        experience2.setDescription("Experience 2");
        experience2.setType(ExperienceType.EDUCATION);

        Experience experience3 = new Experience();
        experience3.setId(3L);
        experience3.setCompany("Company 3");
        experience3.setRole("Role 3");
        experience3.setStartDate(LocalDate.of(2024, 1, 1));
        experience3.setEndDate(null);
        experience3.setDescription("Experience 3");
        experience3.setType(ExperienceType.CERTIFICATION);

        when(experienceRepository.findAllByOrderByStartDateDesc())
                .thenReturn(Arrays.asList(experience3, experience2, testExperience));

        // Act
        List<ExperienceResponse> result = experienceService.getRecentExperiences(2);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(experienceRepository).findAllByOrderByStartDateDesc();
    }
}
