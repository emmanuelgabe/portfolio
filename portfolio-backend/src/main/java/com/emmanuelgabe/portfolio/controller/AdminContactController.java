package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.dto.ContactStatsResponse;
import com.emmanuelgabe.portfolio.moderation.ContactCategory;
import com.emmanuelgabe.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * REST controller for contact moderation administration.
 * Provides endpoints for viewing contact statistics and history.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/contacts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminContactController {

    private final ContactService contactService;

    /**
     * Get contact moderation statistics for the admin dashboard.
     *
     * @param days number of days to include in stats (default 30, max 365)
     * @return statistics with counts by category, verdict, and daily breakdown
     */
    @GetMapping("/stats")
    public ResponseEntity<ContactStatsResponse> getStats(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days
    ) {
        log.debug("[CONTACT_ADMIN] Fetching stats for last {} days", days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        ContactStatsResponse stats = contactService.getContactStats(since);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get paginated contact history with optional category filter.
     *
     * @param category optional filter by contact category
     * @param pageable pagination parameters (default: 20 per page, sorted by createdAt DESC)
     * @return page of contact history entries
     */
    @GetMapping("/history")
    public ResponseEntity<Page<ContactHistoryResponse>> getHistory(
            @RequestParam(required = false) ContactCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.debug("[CONTACT_ADMIN] Fetching contact history - category={}", category);
        Page<ContactHistoryResponse> page = contactService.getContactHistory(category, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get available contact categories for filtering.
     *
     * @return array of all contact categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ContactCategory[]> getCategories() {
        return ResponseEntity.ok(ContactCategory.values());
    }
}
