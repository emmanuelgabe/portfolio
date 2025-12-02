package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.TagResponse;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.entity.Tag;
import com.emmanuelgabe.portfolio.exception.ResourceNotFoundException;
import com.emmanuelgabe.portfolio.mapper.TagMapper;
import com.emmanuelgabe.portfolio.repository.TagRepository;
import com.emmanuelgabe.portfolio.service.impl.TagServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TagServiceImpl
 * Tests all CRUD operations and business logic for tags
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;
    private TagResponse testTagResponse;

    @BeforeEach
    void setUp() {
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Java");
        testTag.setColor("#007396");

        testTagResponse = new TagResponse();
        testTagResponse.setId(1L);
        testTagResponse.setName("Java");
        testTagResponse.setColor("#007396");

        // Mock mapper behavior
        lenient().when(tagMapper.toResponse(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            TagResponse response = new TagResponse();
            response.setId(tag.getId());
            response.setName(tag.getName());
            response.setColor(tag.getColor());
            return response;
        });

        lenient().when(tagMapper.toEntity(any(CreateTagRequest.class))).thenReturn(testTag);
    }

    @Test
    void should_returnListOfTags_when_getAllTagsCalled() {
        // Arrange
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Spring Boot");
        tag2.setColor("#6db33f");

        List<Tag> tags = Arrays.asList(testTag, tag2);
        when(tagRepository.findAll()).thenReturn(tags);

        // Act
        List<TagResponse> result = tagService.getAllTags();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(0).getColor()).isEqualTo("#007396");
        assertThat(result.get(1).getName()).isEqualTo("Spring Boot");
        assertThat(result.get(1).getColor()).isEqualTo("#6db33f");
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    void should_returnEmptyList_when_getAllTagsCalledWithNoTags() {
        // Arrange
        when(tagRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<TagResponse> result = tagService.getAllTags();

        // Assert
        assertThat(result).isEmpty();
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    void should_returnTag_when_getTagByIdCalledWithExistingTag() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));

        // Act
        TagResponse result = tagService.getTagById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getColor()).isEqualTo("#007396");
        verify(tagRepository, times(1)).findById(1L);
    }

    @Test
    void should_throwException_when_getTagByIdCalledWithNonExistentTag() {
        // Arrange
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.getTagById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");
        verify(tagRepository, times(1)).findById(999L);
    }

    @Test
    void should_returnTag_when_getTagByNameCalledWithExistingTag() {
        // Arrange
        when(tagRepository.findByName("Java")).thenReturn(Optional.of(testTag));

        // Act
        TagResponse result = tagService.getTagByName("Java");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getColor()).isEqualTo("#007396");
        verify(tagRepository, times(1)).findByName("Java");
    }

    @Test
    void should_throwException_when_getTagByNameCalledWithNonExistentTag() {
        // Arrange
        when(tagRepository.findByName("Unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.getTagByName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");
        verify(tagRepository, times(1)).findByName("Unknown");
    }

    @Test
    void should_returnCreatedTag_when_createTagCalledWithUniqueName() {
        // Arrange
        CreateTagRequest request = new CreateTagRequest();
        request.setName("Java");
        request.setColor("#007396");

        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        TagResponse result = tagService.createTag(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getColor()).isEqualTo("#007396");
        verify(tagRepository, times(1)).existsByName("Java");
        verify(tagMapper, times(1)).toEntity(request);
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void should_throwException_when_createTagCalledWithExistingName() {
        // Arrange
        CreateTagRequest request = new CreateTagRequest();
        request.setName("Java");
        request.setColor("#007396");

        when(tagRepository.existsByName("Java")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tag with name 'Java' already exists");
        verify(tagRepository, times(1)).existsByName("Java");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void should_returnUpdatedTag_when_updateTagCalledWithExistingTag() {
        // Arrange
        UpdateTagRequest request = new UpdateTagRequest();
        request.setColor("#FF5733");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        TagResponse result = tagService.updateTag(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository, times(1)).findById(1L);
        verify(tagMapper, times(1)).updateEntityFromRequest(request, testTag);
        verify(tagRepository, times(1)).save(testTag);
    }

    @Test
    void should_throwException_when_updateTagCalledWithNonExistentTag() {
        // Arrange
        UpdateTagRequest request = new UpdateTagRequest();
        request.setColor("#FF5733");

        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.updateTag(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");
        verify(tagRepository, times(1)).findById(999L);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void should_updateSuccessfully_when_updateTagCalledWithNewUniqueName() {
        // Arrange
        UpdateTagRequest request = new UpdateTagRequest();
        request.setName("Python");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.existsByName("Python")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        TagResponse result = tagService.updateTag(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).existsByName("Python");
        verify(tagMapper, times(1)).updateEntityFromRequest(request, testTag);
        verify(tagRepository, times(1)).save(testTag);
    }

    @Test
    void should_throwException_when_updateTagCalledWithExistingName() {
        // Arrange
        UpdateTagRequest request = new UpdateTagRequest();
        request.setName("Spring Boot");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.existsByName("Spring Boot")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> tagService.updateTag(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tag with name 'Spring Boot' already exists");
        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).existsByName("Spring Boot");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void should_notCheckDuplicate_when_updateTagCalledWithSameName() {
        // Arrange
        UpdateTagRequest request = new UpdateTagRequest();
        request.setName("Java"); // Same as current name
        request.setColor("#FF5733");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        // Act
        TagResponse result = tagService.updateTag(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, never()).existsByName(anyString()); // Should not check for duplicate
        verify(tagMapper, times(1)).updateEntityFromRequest(request, testTag);
        verify(tagRepository, times(1)).save(testTag);
    }

    @Test
    void should_deleteTag_when_deleteTagCalledWithExistingTag() {
        // Arrange
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTag));
        doNothing().when(tagRepository).delete(testTag);

        // Act
        tagService.deleteTag(1L);

        // Assert
        verify(tagRepository, times(1)).findById(1L);
        verify(tagRepository, times(1)).delete(testTag);
    }

    @Test
    void should_throwException_when_deleteTagCalledWithNonExistentTag() {
        // Arrange
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tagService.deleteTag(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");
        verify(tagRepository, times(1)).findById(999L);
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    @Test
    void should_returnTrue_when_existsByNameCalledWithExistingTag() {
        // Arrange
        when(tagRepository.existsByName("Java")).thenReturn(true);

        // Act
        boolean result = tagService.existsByName("Java");

        // Assert
        assertThat(result).isTrue();
        verify(tagRepository, times(1)).existsByName("Java");
    }

    @Test
    void should_returnFalse_when_existsByNameCalledWithNonExistentTag() {
        // Arrange
        when(tagRepository.existsByName("Unknown")).thenReturn(false);

        // Act
        boolean result = tagService.existsByName("Unknown");

        // Assert
        assertThat(result).isFalse();
        verify(tagRepository, times(1)).existsByName("Unknown");
    }
}
