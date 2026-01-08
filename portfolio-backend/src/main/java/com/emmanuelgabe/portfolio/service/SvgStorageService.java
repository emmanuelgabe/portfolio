package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.config.SvgStorageProperties;
import com.emmanuelgabe.portfolio.exception.FileStorageException;
import com.emmanuelgabe.portfolio.exception.FileValidationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for handling SVG icon uploads for skills.
 * Validates, sanitizes and stores SVG files securely.
 * Protects against XSS and XXE attacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SvgStorageService {

    private final SvgStorageProperties storageProperties;

    private Path uploadPath;

    /**
     * Allowed SVG elements (whitelist approach for security)
     * All element names must be lowercase for case-insensitive matching
     */
    private static final Set<String> ALLOWED_SVG_ELEMENTS = Set.of(
            "svg", "g", "defs", "symbol", "use", "title", "desc",
            "path", "circle", "ellipse", "line", "polyline", "polygon", "rect",
            "lineargradient", "radialgradient", "stop", "clippath", "mask",
            "pattern", "filter", "fegaussianblur", "feoffset", "feblend",
            "femerge", "femergenode", "fecolormatrix", "feflood", "fecomposite",
            // Text elements - needed for SVG logos with text
            "text", "tspan", "textpath",
            // Style element - needed for CSS-styled SVGs (common in icon libraries)
            "style"
    );

    /**
     * Allowed SVG attributes (whitelist approach for security)
     * All attribute names must be lowercase for case-insensitive matching
     */
    private static final Set<String> ALLOWED_SVG_ATTRIBUTES = Set.of(
            "id", "class", "style", "viewbox", "xmlns", "xmlns:xlink",
            "width", "height", "x", "y", "x1", "y1", "x2", "y2",
            "cx", "cy", "r", "rx", "ry", "d", "points",
            "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
            "stroke-dasharray", "stroke-dashoffset", "stroke-opacity", "fill-opacity",
            "opacity", "transform", "clip-path", "mask", "filter",
            "offset", "stop-color", "stop-opacity", "gradientunits", "gradienttransform",
            "spreadmethod", "xlink:href", "href", "preserveaspectratio",
            "patternunits", "patterncontentunits", "patterntransform",
            "stddeviation", "dx", "dy", "result", "in", "in2", "mode",
            "flood-color", "flood-opacity", "operator", "k1", "k2", "k3", "k4",
            "type", "values", "color-interpolation-filters",
            // Text attributes - needed for text elements
            "font-family", "font-size", "font-weight", "font-style", "text-anchor",
            "dominant-baseline", "alignment-baseline", "baseline-shift", "letter-spacing",
            "word-spacing", "text-decoration", "startoffset", "lengthadjust", "textlength",
            // Fill rule for complex paths
            "fill-rule", "clip-rule",
            // Additional presentation attributes
            "color", "display", "visibility", "overflow", "enable-background"
    );

    /**
     * Pattern to detect dangerous content (event handlers, javascript:, vbscript:)
     * Note: data: URIs are checked separately to allow safe image data URIs
     * Uses word boundary \b to avoid false positives (e.g., "standalone=" should not match)
     */
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(\\bon\\w+\\s*=|javascript:|vbscript:)", Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern to detect safe data URIs (only raster images allowed, no SVG to prevent XSS)
     */
    private static final Pattern SAFE_DATA_URI_PATTERN = Pattern.compile(
            "data:image/(png|jpeg|jpg|gif|webp);base64,", Pattern.CASE_INSENSITIVE
    );

    /**
     * Initialize upload directory on service startup
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("[INIT] SVG icon upload directory created - path={}", uploadPath);
        } catch (IOException e) {
            log.error("[INIT] Failed to create SVG upload directory - path={}", storageProperties.getUploadDir(), e);
            throw new FileStorageException("Could not create SVG upload directory", e);
        }
    }

    /**
     * Upload SVG icon for a skill
     *
     * @param skillId Skill ID
     * @param file SVG file to upload
     * @return URL of the uploaded icon
     * @throws FileStorageException if upload fails
     */
    public String uploadSkillIcon(Long skillId, MultipartFile file) {
        log.info("[UPLOAD_SVG] Starting upload - skillId={}, originalFileName={}, size={}",
                skillId, file.getOriginalFilename(), file.getSize());

        // Validate and sanitize SVG file
        String sanitizedSvg = validateAndSanitizeSvg(file);

        try {
            // Delete old icon if exists
            deleteSkillIcon(skillId);

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "skill_" + skillId + "_" + timestamp + ".svg";

            Path targetPath = uploadPath.resolve(fileName);

            // Write sanitized SVG content
            Files.writeString(targetPath, sanitizedSvg, StandardCharsets.UTF_8);

            // Build URL
            String iconUrl = storageProperties.getBasePath() + "/" + fileName;

            log.info("[UPLOAD_SVG] SVG icon uploaded successfully - skillId={}, fileName={}, originalSize={}, sanitizedSize={}",
                    skillId, fileName, file.getSize(), sanitizedSvg.length());

            return iconUrl;

        } catch (IOException e) {
            log.error("[UPLOAD_SVG] Failed to upload SVG icon - skillId={}", skillId, e);
            throw new FileStorageException("Failed to upload SVG icon: " + e.getMessage(), e);
        }
    }

    /**
     * Delete skill icon
     *
     * @param skillId Skill ID
     */
    public void deleteSkillIcon(Long skillId) {
        log.debug("[DELETE_SVG] Deleting icon - skillId={}", skillId);

        try {
            // Find and delete all files matching pattern skill_{id}_*
            Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("skill_" + skillId + "_"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.debug("[DELETE_SVG] Deleted file - fileName={}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("[DELETE_SVG] Failed to delete file - fileName={}", path.getFileName(), e);
                        }
                    });

        } catch (IOException e) {
            log.warn("[DELETE_SVG] Failed to list files for deletion - skillId={}", skillId, e);
        }
    }

    /**
     * Delete icon by URL
     *
     * @param iconUrl URL of the icon to delete
     */
    public void deleteIconByUrl(String iconUrl) {
        if (iconUrl == null || iconUrl.isBlank()) {
            return;
        }

        try {
            // Extract filename from URL
            String fileName = iconUrl.substring(iconUrl.lastIndexOf("/") + 1);
            Path filePath = uploadPath.resolve(fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.debug("[DELETE_SVG] Deleted file - fileName={}", fileName);
            }

        } catch (IOException e) {
            log.warn("[DELETE_SVG] Failed to delete icon - iconUrl={}", iconUrl, e);
        }
    }

    /**
     * Validate and sanitize SVG file.
     * Checks: null/empty, size, extension, MIME type.
     * Sanitizes: removes dangerous elements and attributes to prevent XSS/XXE.
     *
     * @param file File to validate and sanitize
     * @return Sanitized SVG content as string
     * @throws FileStorageException if validation fails
     */
    private String validateAndSanitizeSvg(MultipartFile file) {
        // Check null or empty
        if (file == null || file.isEmpty()) {
            log.warn("[VALIDATION] File is null or empty");
            throw new FileValidationException("File is empty. Please select a valid SVG file.");
        }

        // Security check: prevent path traversal
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains("..")) {
            log.warn("[VALIDATION] Path traversal detected - fileName={}", originalFilename);
            throw new FileValidationException("Invalid file name: path traversal detected");
        }

        // Check file size
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            log.warn("[VALIDATION] File too large - size={}, maxSize={}",
                    file.getSize(), storageProperties.getMaxFileSize());
            throw new FileValidationException(String.format(
                    "File size exceeds maximum allowed size of %d KB",
                    storageProperties.getMaxFileSize() / 1024
            ));
        }

        // Check extension
        if (originalFilename == null || !originalFilename.contains(".")) {
            log.warn("[VALIDATION] Invalid filename - fileName={}", originalFilename);
            throw new FileValidationException("Invalid file name");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!Arrays.asList(storageProperties.getAllowedExtensions()).contains(extension)) {
            log.warn("[VALIDATION] Invalid file extension - fileName={}, extension={}", originalFilename, extension);
            throw new FileValidationException("File type not allowed. Only SVG files are accepted.");
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(storageProperties.getAllowedMimeTypes()).contains(contentType)) {
            log.warn("[VALIDATION] Invalid MIME type - contentType={}", contentType);
            throw new FileValidationException("File type not allowed. Only SVG files are accepted.");
        }

        // Read and sanitize SVG content
        try {
            byte[] fileBytes = file.getBytes();
            String content = new String(fileBytes, StandardCharsets.UTF_8);

            // Check for basic SVG structure
            if (!content.contains("<svg")) {
                log.warn("[VALIDATION] Invalid SVG content - missing svg tag");
                throw new FileValidationException("File is not a valid SVG. Content must contain <svg> element.");
            }

            // Check for dangerous patterns before parsing
            if (DANGEROUS_PATTERN.matcher(content).find()) {
                log.warn("[VALIDATION] Dangerous content detected in SVG - fileName={}", originalFilename);
                throw new FileValidationException("SVG contains potentially dangerous content");
            }

            // Sanitize the SVG
            String sanitizedSvg = sanitizeSvg(content);

            log.debug("[VALIDATION] SVG file validated and sanitized - fileName={}, originalSize={}, sanitizedSize={}",
                    originalFilename, file.getSize(), sanitizedSvg.length());

            return sanitizedSvg;

        } catch (IOException e) {
            log.error("[VALIDATION] Failed to read file bytes", e);
            throw new FileStorageException("Failed to validate SVG file", e);
        }
    }

    /**
     * Sanitize SVG content by removing dangerous elements and attributes.
     * Uses whitelist approach for maximum security.
     *
     * @param svgContent Raw SVG content
     * @return Sanitized SVG content
     */
    private String sanitizeSvg(String svgContent) {
        // Remove XML declaration and DOCTYPE (XXE prevention)
        String cleanContent = svgContent
                .replaceAll("<!DOCTYPE[^>]*>", "")
                .replaceAll("<\\?xml[^>]*\\?>", "")
                .trim();

        // Parse SVG using Jsoup XML parser
        Document doc = Jsoup.parse(cleanContent, "", Parser.xmlParser());

        // Get the SVG element
        Element svgElement = doc.selectFirst("svg");
        if (svgElement == null) {
            throw new FileValidationException("Invalid SVG: no svg element found");
        }

        // Sanitize recursively
        sanitizeElement(svgElement);

        // Remove fixed width/height from root SVG to allow CSS sizing
        // Keep viewBox for proper scaling
        svgElement.removeAttr("width");
        svgElement.removeAttr("height");

        // Return clean SVG with XML declaration
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + svgElement.outerHtml();
    }

    /**
     * Recursively sanitize an SVG element and its children.
     * Removes disallowed elements and attributes.
     *
     * @param element Element to sanitize
     */
    private void sanitizeElement(Element element) {
        // Remove disallowed child elements
        element.children().forEach(child -> {
            String tagName = child.tagName().toLowerCase();
            if (!ALLOWED_SVG_ELEMENTS.contains(tagName)) {
                log.debug("[SANITIZE] Removing disallowed element - tag={}", tagName);
                child.remove();
            } else {
                // Recursively sanitize allowed children
                sanitizeElement(child);
            }
        });

        // Remove disallowed attributes from this element
        element.attributes().asList().forEach(attr -> {
            String attrName = attr.getKey().toLowerCase();
            String attrValue = attr.getValue().toLowerCase();

            // Check if attribute is allowed
            boolean isAllowed = ALLOWED_SVG_ATTRIBUTES.contains(attrName);

            // Check for dangerous values in allowed attributes
            boolean hasDangerousValue = attrValue.contains("javascript:")
                    || attrValue.contains("vbscript:")
                    || DANGEROUS_PATTERN.matcher(attrValue).find()
                    || containsDangerousDataUri(attrValue);

            if (!isAllowed || hasDangerousValue) {
                log.debug("[SANITIZE] Removing attribute - attr={}, dangerous={}", attrName, hasDangerousValue);
                element.removeAttr(attr.getKey());
            }
        });

        // Special handling for style attribute: remove dangerous CSS
        if (element.hasAttr("style")) {
            String style = element.attr("style").toLowerCase();
            if (style.contains("expression") || style.contains("javascript") || style.contains("behavior")) {
                log.debug("[SANITIZE] Removing dangerous style attribute");
                element.removeAttr("style");
            }
        }
    }

    /**
     * Check if a value contains a dangerous data URI.
     * Safe data URIs are image types (png, jpeg, gif, svg+xml, webp).
     *
     * @param value Attribute value to check
     * @return true if contains a dangerous (non-image) data URI
     */
    private boolean containsDangerousDataUri(String value) {
        if (!value.contains("data:")) {
            return false;
        }
        // If it contains data: but is a safe image data URI, it's not dangerous
        if (SAFE_DATA_URI_PATTERN.matcher(value).find()) {
            return false;
        }
        // Any other data: URI is considered dangerous
        log.debug("[SANITIZE] Dangerous data URI detected - value starts with: {}",
                value.substring(0, Math.min(50, value.length())));
        return true;
    }
}
