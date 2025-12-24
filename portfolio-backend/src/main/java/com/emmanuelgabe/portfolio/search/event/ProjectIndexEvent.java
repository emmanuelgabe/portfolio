package com.emmanuelgabe.portfolio.search.event;

import com.emmanuelgabe.portfolio.entity.Project;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event published when a project needs to be indexed or removed from search index.
 */
@Getter
@RequiredArgsConstructor
public class ProjectIndexEvent {

    private final Project project;
    private final IndexAction action;

    public enum IndexAction {
        INDEX,
        REMOVE
    }

    public static ProjectIndexEvent forIndex(Project project) {
        return new ProjectIndexEvent(project, IndexAction.INDEX);
    }

    public static ProjectIndexEvent forRemove(Project project) {
        return new ProjectIndexEvent(project, IndexAction.REMOVE);
    }
}
