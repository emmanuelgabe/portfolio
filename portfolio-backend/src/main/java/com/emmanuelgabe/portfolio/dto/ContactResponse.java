package com.emmanuelgabe.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private String message;
    private boolean success;
    private LocalDateTime timestamp;

    public static ContactResponse success(String message) {
        return new ContactResponse(message, true, LocalDateTime.now());
    }

    public static ContactResponse error(String message) {
        return new ContactResponse(message, false, LocalDateTime.now());
    }
}
