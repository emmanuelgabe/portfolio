package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.FileUploadResponse;
import com.emmanuelgabe.portfolio.mapper.FileUploadMapper;
import com.emmanuelgabe.portfolio.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for file upload operations
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "File upload management endpoints (Admin only)")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final FileUploadMapper fileUploadMapper;

    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload image", description = "Upload an image file (Admin only)")
    public ResponseEntity<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("[CREATE_FILE] Request received - fileName={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        String fileName = fileStorageService.storeFile(file);
        String fileUrl = fileStorageService.getFileUrl(fileName);

        FileUploadResponse response = fileUploadMapper.toResponse(
                fileName,
                fileUrl,
                file.getContentType(),
                file.getSize()
        );

        log.info("[CREATE_FILE] Success - fileName={}, fileUrl={}", fileName, fileUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
