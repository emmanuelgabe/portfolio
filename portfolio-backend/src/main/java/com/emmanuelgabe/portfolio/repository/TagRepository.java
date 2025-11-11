package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Find tag by name
     * @param name The tag name
     * @return Optional containing the tag if found
     */
    Optional<Tag> findByName(String name);

    /**
     * Check if a tag with the given name exists
     * @param name The tag name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
