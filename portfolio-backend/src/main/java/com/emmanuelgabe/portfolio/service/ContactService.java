package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.dto.ContactStatsResponse;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ContactService {

    /**
     * Send contact email to configured address.
     *
     * @param request contact form data
     * @return response with success status
     */
    ContactResponse sendContactEmail(ContactRequest request);

    /**
     * Get aggregated contact moderation statistics for the admin dashboard.
     *
     * @param since start date for the statistics window
     * @return statistics with counts by category, verdict, and daily breakdown
     */
    ContactStatsResponse getContactStats(LocalDateTime since);

    /**
     * Get paginated contact history with optional category filter.
     *
     * @param category optional filter by contact category (null for all)
     * @param pageable pagination and sort parameters
     * @return page of contact history entries
     */
    Page<ContactHistoryResponse> getContactHistory(ContactCategory category, Pageable pageable);
}
