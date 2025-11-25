package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;
import com.emmanuelgabe.portfolio.service.ContactService;
import com.emmanuelgabe.portfolio.service.RateLimitService;
import com.emmanuelgabe.portfolio.util.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.contact.max-requests-per-hour:5}")
    private int maxRequestsPerHour;

    @PostMapping
    public ResponseEntity<ContactResponse> sendContactMessage(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpAddressExtractor.extractIpAddress(httpRequest);
        log.info("[CONTACT] Request received - ip={}, email={}", ip, request.getEmail());

        // Check rate limiting
        if (!rateLimitService.isAllowed(ip)) {
            long remaining = rateLimitService.getRemainingAttempts(ip);
            log.warn("[CONTACT] Rate limit exceeded - ip={}, remaining={}", ip, remaining);

            String errorMessage = String.format(
                    "Rate limit exceeded. You can send maximum %d messages per hour. Please try again later.",
                    maxRequestsPerHour
            );
            ContactResponse response = ContactResponse.error(errorMessage);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        // Send email
        ContactResponse response = contactService.sendContactEmail(request);
        log.info("[CONTACT] Success - ip={}, email={}", ip, request.getEmail());

        return ResponseEntity.ok(response);
    }
}
