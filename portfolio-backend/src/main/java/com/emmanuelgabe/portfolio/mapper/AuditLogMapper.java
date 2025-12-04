package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.audit.AuditLogResponse;
import com.emmanuelgabe.portfolio.entity.AuditLog;
import com.emmanuelgabe.portfolio.messaging.event.AuditEvent;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for AuditLog entity.
 */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    /**
     * Convert AuditLog entity to response DTO.
     *
     * @param auditLog the audit log entity
     * @return the response DTO
     */
    @Mapping(target = "actionDescription", expression = "java(auditLog.getAction().getDescription())")
    AuditLogResponse toResponse(AuditLog auditLog);

    /**
     * Convert AuditEvent to AuditLog entity.
     *
     * @param event the audit event from RabbitMQ
     * @return the audit log entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    AuditLog toEntity(AuditEvent event);

    /**
     * Set default values after mapping.
     */
    @AfterMapping
    default void setDefaults(AuditEvent event, @MappingTarget AuditLog auditLog) {
        if (auditLog.getUsername() == null) {
            auditLog.setUsername("system");
        }
    }
}
