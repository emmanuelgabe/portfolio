package com.emmanuelgabe.portfolio.graphql.type.response;

import com.emmanuelgabe.portfolio.dto.ExperienceResponse;
import com.emmanuelgabe.portfolio.entity.ExperienceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Group of experiences by type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceTypeGroup {

    private ExperienceType type;
    private List<ExperienceResponse> experiences;
}
