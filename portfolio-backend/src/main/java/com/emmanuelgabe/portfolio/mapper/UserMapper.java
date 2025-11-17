package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.AuthResponse;
import com.emmanuelgabe.portfolio.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for User entity
 * Converts between User entity and DTOs
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Map User to AuthResponse (without tokens)
     * Tokens should be added separately by service
     */
    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "tokenType", ignore = true)
    @Mapping(target = "expiresIn", ignore = true)
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    AuthResponse toAuthResponse(User user);
}
