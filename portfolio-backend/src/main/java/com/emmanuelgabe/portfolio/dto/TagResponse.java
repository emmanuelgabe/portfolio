package com.emmanuelgabe.portfolio.dto;

import com.emmanuelgabe.portfolio.entity.Tag;

/**
 * Data Transfer Object for Tag entity.
 * Used to serialize tag information in API responses.
 */
public class TagResponse {

    private Long id;
    private String name;
    private String color;

    public TagResponse() {
    }

    public TagResponse(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    /**
     * Converts a Tag entity to a TagResponse DTO.
     *
     * @param tag the Tag entity to convert
     * @return the corresponding TagResponse DTO, or null if the input tag is null
     */
    public static TagResponse fromEntity(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
