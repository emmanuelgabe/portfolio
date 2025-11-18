package com.emmanuelgabe.portfolio.mapper;

import com.emmanuelgabe.portfolio.dto.FileUploadResponse;
import org.mapstruct.Mapper;

/**
 * Mapper for file upload operations
 */
@Mapper(componentModel = "spring")
public interface FileUploadMapper {

    /**
     * Map file upload details to response DTO
     * @param fileName Name of the uploaded file
     * @param fileUrl URL to access the file
     * @param contentType MIME type of the file
     * @param size Size of the file in bytes
     * @return FileUploadResponse DTO
     */
    default FileUploadResponse toResponse(String fileName, String fileUrl, String contentType, long size) {
        return new FileUploadResponse(fileName, fileUrl, contentType, size);
    }
}
