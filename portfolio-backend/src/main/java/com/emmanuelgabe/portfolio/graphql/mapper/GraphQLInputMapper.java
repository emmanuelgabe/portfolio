package com.emmanuelgabe.portfolio.graphql.mapper;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.CreateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.CreateProjectRequest;
import com.emmanuelgabe.portfolio.dto.CreateSkillRequest;
import com.emmanuelgabe.portfolio.dto.CreateTagRequest;
import com.emmanuelgabe.portfolio.dto.UpdateExperienceRequest;
import com.emmanuelgabe.portfolio.dto.UpdateProjectRequest;
import com.emmanuelgabe.portfolio.dto.UpdateSiteConfigurationRequest;
import com.emmanuelgabe.portfolio.dto.UpdateSkillRequest;
import com.emmanuelgabe.portfolio.dto.UpdateTagRequest;
import com.emmanuelgabe.portfolio.dto.article.CreateArticleRequest;
import com.emmanuelgabe.portfolio.dto.article.UpdateArticleRequest;
import com.emmanuelgabe.portfolio.entity.IconType;
import com.emmanuelgabe.portfolio.graphql.input.ContactInput;
import com.emmanuelgabe.portfolio.graphql.input.CreateArticleInput;
import com.emmanuelgabe.portfolio.graphql.input.CreateExperienceInput;
import com.emmanuelgabe.portfolio.graphql.input.CreateProjectInput;
import com.emmanuelgabe.portfolio.graphql.input.CreateSkillInput;
import com.emmanuelgabe.portfolio.graphql.input.CreateTagInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateArticleInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateExperienceInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateProjectInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateSiteConfigurationInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateSkillInput;
import com.emmanuelgabe.portfolio.graphql.input.UpdateTagInput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for GraphQL input types to DTO request conversions.
 * Centralizes all GraphQL input -> request mappings to avoid manual mapping in resolvers.
 */
@Mapper(componentModel = "spring")
public interface GraphQLInputMapper {

    // ========== Project Mappings ==========

    @Mapping(target = "featured", source = "featured", qualifiedByName = "booleanToFeatured")
    @Mapping(target = "hasDetails", source = "hasDetails", qualifiedByName = "booleanToHasDetails")
    CreateProjectRequest toCreateProjectRequest(CreateProjectInput input);

    UpdateProjectRequest toUpdateProjectRequest(UpdateProjectInput input);

    // ========== Article Mappings ==========

    @Mapping(target = "draft", source = "draft", qualifiedByName = "booleanToDraft")
    CreateArticleRequest toCreateArticleRequest(CreateArticleInput input);

    @Mapping(target = "draft", ignore = true)
    UpdateArticleRequest toUpdateArticleRequest(UpdateArticleInput input);

    // ========== Skill Mappings ==========

    @Mapping(target = "iconType", source = "iconType", qualifiedByName = "iconTypeWithDefault")
    CreateSkillRequest toCreateSkillRequest(CreateSkillInput input);

    UpdateSkillRequest toUpdateSkillRequest(UpdateSkillInput input);

    // ========== Experience Mappings ==========

    @Mapping(target = "showMonths", source = "showMonths", qualifiedByName = "booleanToShowMonths")
    CreateExperienceRequest toCreateExperienceRequest(CreateExperienceInput input);

    UpdateExperienceRequest toUpdateExperienceRequest(UpdateExperienceInput input);


    // ========== Tag Mappings ==========

    CreateTagRequest toCreateTagRequest(CreateTagInput input);

    UpdateTagRequest toUpdateTagRequest(UpdateTagInput input);

    // ========== Site Configuration Mappings ==========

    UpdateSiteConfigurationRequest toUpdateSiteConfigurationRequest(UpdateSiteConfigurationInput input);

    // ========== Contact Mappings ==========

    ContactRequest toContactRequest(ContactInput input);

    // ========== Helper Methods ==========

    @Named("booleanToFeatured")
    default boolean booleanToFeatured(Boolean featured) {
        return featured != null ? featured : false;
    }

    @Named("booleanToHasDetails")
    default boolean booleanToHasDetails(Boolean hasDetails) {
        return hasDetails != null ? hasDetails : true;
    }

    @Named("booleanToDraft")
    default Boolean booleanToDraft(Boolean draft) {
        return draft != null ? draft : true;
    }

    @Named("iconTypeWithDefault")
    default IconType iconTypeWithDefault(IconType iconType) {
        return iconType != null ? iconType : IconType.FONT_AWESOME;
    }

    @Named("booleanToShowMonths")
    default Boolean booleanToShowMonths(Boolean showMonths) {
        return showMonths != null ? showMonths : true;
    }
}

