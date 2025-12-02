package com.emmanuelgabe.portfolio.repository;

import com.emmanuelgabe.portfolio.entity.SiteConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SiteConfiguration entity.
 * Only one row exists (id = 1), enforced by database constraint.
 */
@Repository
public interface SiteConfigurationRepository extends JpaRepository<SiteConfiguration, Long> {
}
