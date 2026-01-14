package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.ReorderRequest;
import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.entity.IconType;
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

    @Mock
    private SvgStorageService svgStorageService;

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
            response.setDisplayOrder(skill.getDisplayOrder());
            response.setCreatedAt(skill.getCreatedAt());
            response.setUpdatedAt(skill.getUpdatedAt());
            return response;
        });

        lenient().when(skillMapper.toEntity(any(CreateSkillRequest.class))).thenReturn(testSkill);
    }

    @Test
    void should_returnListOfSkillsOrderedByDisplayOrder_when_getAllSkillsCalled() {
        // Arrange
        Skill skill2 = new Skill();
        skill2.setId(2L);
        skill2.setName("Spring Boot");
        skill2.setIcon("bi-gear");
        skill2.setColor("#6db33f");
        skill2.setCategory(SkillCategory.BACKEND);
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
    void should_returnSkill_when_getSkillByIdCalledWithExistingSkill() {
        // Arrange
        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));

        // Act
        SkillResponse result = skillService.getSkillById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getCategory()).isEqualTo(SkillCategory.BACKEND);
        verify(skillRepository, times(1)).findById(1L);
    }

    @Test
    void should_throwException_when_getSkillByIdCalledWithNonExistentSkill() {
        // Arrange
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.getSkillById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
        verify(skillRepository, times(1)).findById(999L);
    }

    @Test
    void should_returnCreatedSkill_when_createSkillCalled() {
        // Arrange
        CreateSkillRequest request = new CreateSkillRequest();
        request.setName("Java");
        request.setIcon("bi-cup-hot");
        request.setColor("#007396");
        request.setCategory(SkillCategory.BACKEND);
        request.setDisplayOrder(1);

        when(skillRepository.save(any(Skill.class))).thenReturn(testSkill);

        // Act
        SkillResponse result = skillService.createSkill(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getCategory()).isEqualTo(SkillCategory.BACKEND);
        verify(skillMapper, times(1)).toEntity(request);
        verify(skillRepository, times(1)).save(any(Skill.class));
    }

    @Test
    void should_returnUpdatedSkill_when_updateSkillCalledWithExistingSkill() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
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
    void should_throwException_when_updateSkillCalledWithNonExistentSkill() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setDisplayOrder(2);

        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.updateSkill(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
        verify(skillRepository, times(1)).findById(999L);
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void should_updateOnlyProvidedFields_when_updateSkillCalledWithPartialData() {
        // Arrange
        UpdateSkillRequest request = new UpdateSkillRequest();
        request.setDisplayOrder(2); // Only update display order

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
    void should_deleteSkill_when_deleteSkillCalledWithExistingSkill() {
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
    void should_throwException_when_deleteSkillCalledWithNonExistentSkill() {
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
    void should_returnSkillsOfCategory_when_getSkillsByCategoryCalled() {
        // Arrange
        Skill backendSkill2 = new Skill();
        backendSkill2.setId(2L);
        backendSkill2.setName("Spring Boot");
        backendSkill2.setIcon("bi-gear");
        backendSkill2.setColor("#6db33f");
        backendSkill2.setCategory(SkillCategory.BACKEND);
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
    void should_returnEmptyList_when_getSkillsByCategoryCalledWithNoSkillsInCategory() {
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
    void should_returnEmptyList_when_getAllSkillsCalledWithNoSkills() {
        // Arrange
        when(skillRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(Arrays.asList());

        // Act
        List<SkillResponse> result = skillService.getAllSkills();

        // Assert
        assertThat(result).isEmpty();
        verify(skillRepository, times(1)).findAllByOrderByDisplayOrderAsc();
    }

    // ========== Reorder Tests ==========

    @Test
    void should_updateDisplayOrder_when_reorderSkillsCalled() {
        // Arrange
        Skill skill1 = new Skill();
        skill1.setId(1L);
        skill1.setName("Java");
        skill1.setDisplayOrder(0);

        Skill skill2 = new Skill();
        skill2.setId(2L);
        skill2.setName("Spring");
        skill2.setDisplayOrder(1);

        Skill skill3 = new Skill();
        skill3.setId(3L);
        skill3.setName("Docker");
        skill3.setDisplayOrder(2);

        ReorderRequest request = new ReorderRequest();
        request.setOrderedIds(Arrays.asList(3L, 1L, 2L));

        when(skillRepository.findById(3L)).thenReturn(Optional.of(skill3));
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill1));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill2));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        skillService.reorderSkills(request);

        // Assert
        assertThat(skill3.getDisplayOrder()).isEqualTo(0);
        assertThat(skill1.getDisplayOrder()).isEqualTo(1);
        assertThat(skill2.getDisplayOrder()).isEqualTo(2);
        verify(skillRepository, times(3)).save(any(Skill.class));
    }

    @Test
    void should_throwException_when_reorderSkillsCalledWithNonExistentSkill() {
        // Arrange
        ReorderRequest request = new ReorderRequest();
        request.setOrderedIds(Arrays.asList(1L, 999L));

        when(skillRepository.findById(1L)).thenReturn(Optional.of(testSkill));
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> skillService.reorderSkills(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Skill not found");
    }

    // ========== Delete with Custom Icon Tests ==========

    @Test
    void should_deleteCustomIcon_when_deleteSkillCalledWithCustomSvgIcon() {
        // Arrange
        Skill skillWithIcon = new Skill();
        skillWithIcon.setId(2L);
        skillWithIcon.setName("Custom Skill");
        skillWithIcon.setIconType(IconType.CUSTOM_SVG);
        skillWithIcon.setCustomIconUrl("/uploads/icons/skill_2_123.svg");

        when(skillRepository.findById(2L)).thenReturn(Optional.of(skillWithIcon));
        doNothing().when(svgStorageService).deleteIconByUrl(skillWithIcon.getCustomIconUrl());
        doNothing().when(skillRepository).delete(skillWithIcon);

        // Act
        skillService.deleteSkill(2L);

        // Assert
        verify(svgStorageService, times(1)).deleteIconByUrl("/uploads/icons/skill_2_123.svg");
        verify(skillRepository, times(1)).delete(skillWithIcon);
    }
}
