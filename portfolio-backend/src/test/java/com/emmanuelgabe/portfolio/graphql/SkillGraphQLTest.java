package com.emmanuelgabe.portfolio.graphql;

import com.emmanuelgabe.portfolio.dto.SkillResponse;
import com.emmanuelgabe.portfolio.entity.IconType;
import com.emmanuelgabe.portfolio.entity.SkillCategory;
import com.emmanuelgabe.portfolio.graphql.config.GraphQLConfig;
import com.emmanuelgabe.portfolio.graphql.resolver.query.SkillQueryResolver;
import com.emmanuelgabe.portfolio.service.SkillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * GraphQL integration tests for Skill queries.
 */
@GraphQlTest(SkillQueryResolver.class)
@Import(GraphQLConfig.class)
class SkillGraphQLTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private SkillService skillService;

    // ========== skills Query Tests ==========

    @Test
    void should_returnSkills_when_skillsQueryExecuted() {
        // Arrange
        SkillResponse skill = createTestSkill();
        when(skillService.getAllSkills()).thenReturn(List.of(skill));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        skills {
                            id
                            name
                            category
                            categoryDisplayName
                            color
                        }
                    }
                """)
                .execute()
                .path("skills")
                .entityList(SkillResponse.class)
                .hasSize(1)
                .path("skills[0].name")
                .entity(String.class)
                .isEqualTo("Java");
    }

    @Test
    void should_returnEmptyList_when_noSkillsExist() {
        // Arrange
        when(skillService.getAllSkills()).thenReturn(List.of());

        // Act & Assert
        graphQlTester.document("""
                    query {
                        skills {
                            id
                            name
                        }
                    }
                """)
                .execute()
                .path("skills")
                .entityList(Object.class)
                .hasSize(0);
    }

    // ========== skill Query Tests ==========

    @Test
    void should_returnSkill_when_skillQueryExecutedWithValidId() {
        // Arrange
        SkillResponse skill = createTestSkill();
        when(skillService.getSkillById(1L)).thenReturn(skill);

        // Act & Assert
        graphQlTester.document("""
                    query {
                        skill(id: 1) {
                            id
                            name
                            category
                            iconType
                            displayOrder
                        }
                    }
                """)
                .execute()
                .path("skill.id")
                .entity(Long.class)
                .isEqualTo(1L)
                .path("skill.name")
                .entity(String.class)
                .isEqualTo("Java")
                .path("skill.category")
                .entity(String.class)
                .isEqualTo("BACKEND");
    }

    // ========== skillsByCategory Query Tests ==========

    @Test
    void should_returnSkillsGroupedByCategory_when_skillsByCategoryQueryExecuted() {
        // Arrange
        SkillResponse backendSkill = createTestSkill();
        SkillResponse frontendSkill = createTestSkill();
        frontendSkill.setId(2L);
        frontendSkill.setName("Angular");
        frontendSkill.setCategory(SkillCategory.FRONTEND);
        frontendSkill.setCategoryDisplayName("Frontend");

        when(skillService.getAllSkills()).thenReturn(List.of(backendSkill, frontendSkill));

        // Act & Assert
        graphQlTester.document("""
                    query {
                        skillsByCategory {
                            category
                            categoryDisplayName
                            skills {
                                id
                                name
                            }
                        }
                    }
                """)
                .execute()
                .path("skillsByCategory")
                .entityList(Object.class)
                .hasSize(2);
    }

    private SkillResponse createTestSkill() {
        SkillResponse skill = new SkillResponse();
        skill.setId(1L);
        skill.setName("Java");
        skill.setIcon("fa-code");
        skill.setIconType(IconType.FONT_AWESOME);
        skill.setColor("#007396");
        skill.setCategory(SkillCategory.BACKEND);
        skill.setCategoryDisplayName("Backend");
        skill.setDisplayOrder(1);
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        return skill;
    }
}
