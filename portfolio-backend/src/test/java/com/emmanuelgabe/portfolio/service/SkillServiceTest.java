package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.Skill;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.SkillMapper;
import com.emmanuelgabe.portfolio.repository.SkillRepository;
import com.emmanuelgabe.portfolio.service.impl.SkillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SkillServiceImpl
 * Tests all CRUD operations and business logic for skills
 */
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private SkillServiceImpl skillService;

    private Skill testSkill;
    private SkillResponse testSkillResponse;

    @BeforeEach
    void setUp() {
        testSkill = new Skill();
        testSkill.setId(1L);
        testSkill.setName("Java");
        testSkill.setIcon("bi-cup-hot");
        testSkill.setColor("#007396");
        testSkill.setCategory(SkillCategory.BACKEND);
        testSkill.setLevel(90);
        testSkill.setDisplayOrder(1);
        testSkill.setCreatedAt(LocalDateTime.now());
        testSkill.setUpdatedAt(LocalDateTime.now());

        testSkillResponse = new SkillResponse();
        testSkillResponse.setId(1L);
        testSkillResponse.setName("Java");
        testSkillResponse.setIcon("bi-cup-hot");
        testSkillResponse.setColor("#007396");
        testSkillResponse.setCategory(SkillCategory.BACKEND);
        testSkillResponse.setCategoryDisplayName(SkillCategory.BACKEND.getDisplayName());
        testSkillResponse.setLevel(90);
        testSkillResponse.setDisplayOrder(1);
        testSkillResponse.setCreatedAt(testSkill.getCreatedAt());
        testSkillResponse.setUpdatedAt(testSkill.getUpdatedAt());

        // Mock mapper behavior
        lenient().when(skillMapper.toResponse(any(Skill.class))).thenAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            SkillResponse response = new SkillResponse();
            response.setId(skill.getId());
            response.setName(skill.getName());
            response.setIcon(skill.getIcon());
            response.setColor(skill.getColor());
            response.setCategory(skill.getCategory());
            response.setCategoryDisplayName(skill.getCategory() != null ? skill.getCategory().getDisplayName() : null);
            response.setLevel(skill.getLevel());
            response.setDisplayOrder(skill.getDisplayOrder());
            response.setCreatedAt(skill.getCreatedAt());
            response.setUpdatedAt(skill.getUpdatedAt());
            return response;
        });

        lenient().when(skillMapper.toEntity(any(CreateSkillRequest.class))).thenReturn(testSkill);
    }

    @Test
    void getAllSkills_ShouldReturnListOfSkillsOrderedByDisplayOrder() {
        // Arrange
        Skill skill2 = new Skill();
        skill2.setId(2L);
        skill2.setName("Spring Boot");
        skill2.setIcon("bi-gear");
        skill2.setColor("#6db33f");
        skill2.setCategory(SkillCategory.BACKEND);
        skill2.setLevel(85);
        skill2.setDisplayOrder(2);

        List<Skill> skills = Arrays.asList(testSkill, skill2);
        when(skillRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(skills);

        // Act
        List<SkillResponse> result = skillService.getAllSkills();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(result.get(1).getName()).isEqualTo("Spring Boot");
        assertThat(result.get(1).getDisplayOrder()).isEqualTo(2);
        verify(skillRepository, times(1)).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    void getSkillById_WhenSkillExists_ShouldReturnSkill() {
        // Arrange
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));

        // Act
        SkillResponse result = skillService.getSkillById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getCategory()).isEqualTo(SkillCategory.BACKEND);
        assertThat(result.getLevel()).isEqualTo(90);
        verify(skillRepository, times(1)).findById(1L);
    }

    @Test
    void getSkillById_WhenSkillNotFound_ShouldThrowException() {
        // Arrange
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.getSkillById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
        verify(skillRepository, times(1)).findById(999L);
    }

    @Test
    void createSkill_ShouldReturnCreatedSkill() {
        // Arrange
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setIcon("bi-cup-hot");
        request.setColor("#007396");
        request.setCategory(SkillCategory.BACKEND);
        request.setLevel(90);
        request.setDisplayOrder(1);

        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        // Act
        SkillResponse result = skillService.createSkill(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getCategory()).isEqualTo(SkillCategory.BACKEND);
        assertThat(result.getLevel()).isEqualTo(90);
        verify(skillMapper, times(1)).toEntity(request);
        verify(skillRepository, times(1)).save(any(Skill.class));
    }

    @Test
    void updateSkill_WhenSkillExists_ShouldReturnUpdatedSkill() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setLevel(95);
        request.setDisplayOrder(2);

        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        // Act
        SkillResponse result = skillService.updateSkill(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(skillRepository, times(1)).findById(1L);
        verify(skillMapper, times(1)).updateEntityFromRequest(request, testSkill);
        verify(skillRepository, times(1)).save(testSkill);
    }

    @Test
    void updateSkill_WhenSkillNotFound_ShouldThrowException() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setLevel(95);

        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.updateSkill(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
        verify(skillRepository, times(1)).findById(999L);
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void updateSkill_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setLevel(95); // Only update level

        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        // Act
        SkillResponse result = skillService.updateSkill(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(skillMapper, times(1)).updateEntityFromRequest(request, testSkill);
        verify(skillRepository, times(1)).save(testSkill);
    }

    @Test
    void deleteSkill_WhenSkillExists_ShouldDeleteSkill() {
        // Arrange
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        doNothing().when(skillRepository).delete(testSkill);

        // Act
        skillService.deleteSkill(1L);

        // Assert
        verify(skillRepository, times(1)).findById(1L);
        verify(skillRepository, times(1)).delete(testSkill);
    }

    @Test
    void deleteSkill_WhenSkillNotFound_ShouldThrowException() {
        // Arrange
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.deleteSkill(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
        verify(skillRepository, times(1)).findById(999L);
        verify(skillRepository, never()).delete(any(Skill.class));
    }

    @Test
    void getSkillsByCategory_ShouldReturnSkillsOfCategory() {
        // Arrange
        Skill backendSkill2 = new Skill();
        backendSkill2.setId(2L);
        backendSkill2.setName("Spring Boot");
        backendSkill2.setIcon("bi-gear");
        backendSkill2.setColor("#6db33f");
        backendSkill2.setCategory(SkillCategory.BACKEND);
        backendSkill2.setLevel(85);
        backendSkill2.setDisplayOrder(2);

        List<Skill> backendSkills = Arrays.asList(testSkill, backendSkill2);
        when(skillRepository.findByCategoryOrderByDisplayOrderAsc(SkillCategory.BACKEND))
                .thenReturn(backendSkills);

        // Act
        List<SkillResponse> result = skillService.getSkillsByCategory(SkillCategory.BACKEND);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory()).isEqualTo(SkillCategory.BACKEND);
        assertThat(result.get(1).getCategory()).isEqualTo(SkillCategory.BACKEND);
        assertThat(result.get(0).getDisplayOrder()).isLessThan(result.get(1).getDisplayOrder());
        verify(skillRepository, times(1)).findByCategoryOrderByDisplayOrderAsc(SkillCategory.BACKEND);
    }

    @Test
    void getSkillsByCategory_WhenNoSkillsInCategory_ShouldReturnEmptyList() {
        // Arrange
        when(skillRepository.findByCategoryOrderByDisplayOrderAsc(SkillCategory.DEVOPS))
                .thenReturn(Arrays.asList());

        // Act
        List<SkillResponse> result = skillService.getSkillsByCategory(SkillCategory.DEVOPS);

        // Assert
        assertThat(result).isEmpty();
        verify(skillRepository, times(1)).findByCategoryOrderByDisplayOrderAsc(SkillCategory.DEVOPS);
    }

    @Test
    void getAllSkills_WhenNoSkills_ShouldReturnEmptyList() {
        // Arrange
        when(skillRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(Arrays.asList());

        // Act
        List<SkillResponse> result = skillService.getAllSkills();

        // Assert
        assertThat(result).isEmpty();
        verify(skillRepository, times(1)).findAllByOrderByDisplayOrderAsc();
    }
}
