package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.ContactHistoryResponse;
import com.emmanuelgabe.portfolio.entity.Contact;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Contact entity.
 */
@Mapper(componentModel = "spring")
public interface ContactMapper {

    /**
     * Convert Contact entity to history response DTO.
     *
     * @param contact the contact entity
     * @return the history response DTO
     */
    ContactHistoryResponse toHistoryResponse(Contact contact);
}
