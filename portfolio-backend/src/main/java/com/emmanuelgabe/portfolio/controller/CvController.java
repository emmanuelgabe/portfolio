package com.emmanuelgabe.portfolio.controller;

import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.entity.User;
import com.emmanuelgabe.portfolio.service.CvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Controller for CV management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
@Tag(name = "CV Management", description = "CV upload, download and version management endpoints")
public class CvController {

    private final CvService cvService;

    /**
     * Upload a new CV
     * Only accessible to admin users
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload CV", description = "Upload a new CV file (PDF only). Automatically sets it as current. Admin only.")
    public ResponseEntity<CvResponse> uploadCv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        log.info("[CREATE_CV] Upload request received - userId={}, fileName={}, size={}",
                user.getId(), file.getOriginalFilename(), file.getSize());

        CvResponse response = cvService.uploadCv(file, user);

        log.info("[CREATE_CV] CV uploaded successfully - cvId={}, userId={}, fileName={}",
                response.getId(), user.getId(), response.getFileName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current CV metadata
     * Public endpoint
     */
    @GetMapping("/current")
    @Operation(summary = "Get current CV", description = "Get metadata of the current CV (public endpoint)")
    public ResponseEntity<CvResponse> getCurrentCv() {
        log.debug("[GET_CURRENT_CV] Request received");

        Optional<CvResponse> currentCv = cvService.getCurrentCv();

        if (currentCv.isPresent()) {
            log.info("[GET_CURRENT_CV] Current CV found - cvId={}", currentCv.get().getId());
            return ResponseEntity.ok(currentCv.get());
        } else {
            log.debug("[GET_CURRENT_CV] No current CV found");
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Download current CV file
     * Public endpoint - returns the PDF file
     */
    @GetMapping("/download")
    @Operation(summary = "Download current CV", description = "Download the current CV file as PDF (public endpoint)")
    public ResponseEntity<Resource> downloadCv() {
        log.info("[DOWNLOAD_CV] Download request received");

        Resource resource = cvService.downloadCurrentCv();

        log.info("[DOWNLOAD_CV] Download successful");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"emmanuel_gabe_cv_developpeur_backend.pdf\"")
                .body(resource);
    }

    /**
     * Get all CVs for the authenticated user
     * Admin only
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all CVs", description = "Get all CV versions for the authenticated user. Admin only.")
    public ResponseEntity<List<CvResponse>> getAllCvs(@AuthenticationPrincipal User user) {
        log.debug("[LIST_CV] Request received - userId={}", user.getId());

        List<CvResponse> cvs = cvService.getAllCvs(user.getId());

        log.info("[LIST_CV] CVs retrieved - userId={}, count={}", user.getId(), cvs.size());

        return ResponseEntity.ok(cvs);
    }

    /**
     * Set a specific CV as current
     * Admin only
     */
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set CV as current", description = "Set a specific CV version as current. Admin only.")
    public ResponseEntity<CvResponse> setCurrentCv(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        log.info("[SET_CURRENT_CV] Request received - cvId={}, userId={}", id, user.getId());

        CvResponse response = cvService.setCurrentCv(id, user);

        log.info("[SET_CURRENT_CV] CV set as current - cvId={}, userId={}", id, user.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a CV
     * Admin only
     * Cannot delete current CV if other versions exist
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete CV", description = "Delete a CV version. Cannot delete current CV if other versions exist. Admin only.")
    public ResponseEntity<Void> deleteCv(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        log.info("[DELETE_CV] Request received - cvId={}, userId={}", id, user.getId());

        cvService.deleteCv(id, user);

        log.info("[DELETE_CV] CV deleted successfully - cvId={}, userId={}", id, user.getId());

        return ResponseEntity.noContent().build();
    }
}
