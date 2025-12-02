package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.ContactRequest;
import com.emmanuelgabe.portfolio.dto.ContactResponse;

public interface ContactService {

    /**
     * Send contact email to configured address
     *
     * @param request Contact form data
     * @return Response with success status
     */
    ContactResponse sendContactEmail(ContactRequest request);
}
