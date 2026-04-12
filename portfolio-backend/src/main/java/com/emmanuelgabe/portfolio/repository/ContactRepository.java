package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.Contact;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for contact form submissions with moderation results.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Page<Contact> findByCategory(ContactCategory category, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contact c WHERE c.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT c.category, COUNT(c) FROM Contact c "
            + "WHERE c.createdAt >= :since GROUP BY c.category")
    List<Object[]> countByCategorySince(@Param("since") LocalDateTime since);

    @Query("SELECT c.moderationVerdict, COUNT(c) FROM Contact c "
            + "WHERE c.createdAt >= :since GROUP BY c.moderationVerdict")
    List<Object[]> countByVerdictSince(@Param("since") LocalDateTime since);

    @Query(value = "SELECT c.created_at::date AS day, COUNT(*) FROM contacts c "
            + "WHERE c.created_at >= :since "
            + "GROUP BY day ORDER BY day",
            nativeQuery = true)
    List<Object[]> countDailySince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM Contact c WHERE c.createdAt < :date")
    int deleteExpiredBefore(@Param("date") LocalDateTime date);
}
