package com.emmanuelgabe.portfolio.search.event;

import com.emmanuelgabe.portfolio.entity.Experience;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when an experience needs to be indexed or removed from search index.
 */
@Getter
@RequiredArgsConstructor
public class ExperienceIndexEvent {

    private final Experience experience;
    private final IndexAction action;

    public enum IndexAction {
        INDEX,
        REMOVE
    }

    public static ExperienceIndexEvent forIndex(Experience experience) {
        return new ExperienceIndexEvent(experience, IndexAction.INDEX);
    }

    public static ExperienceIndexEvent forRemove(Experience experience) {
        return new ExperienceIndexEvent(experience, IndexAction.REMOVE);
    }
}
