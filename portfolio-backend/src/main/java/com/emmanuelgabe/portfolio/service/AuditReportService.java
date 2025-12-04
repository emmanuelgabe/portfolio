package com.emmanuelgabe.portfolio.service;

import com.emmanuelgabe.portfolio.dto.audit.AuditStatsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for generating audit reports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditReportService {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${batch.audit-report.output-dir:reports/audit}")
    private String outputDir;

    /**
     * Generate monthly audit report for the previous month.
     *
     * @return the generated report file
     * @throws IOException if report generation fails
     */
    public File generateMonthlyReport() throws IOException {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDateTime startOfMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        log.info("[AUDIT_REPORT] Generating monthly report - month={}", previousMonth);

        // Get statistics for the previous month
        AuditStatsResponse stats = auditService.getStats(startOfMonth);

        // Build report object
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "MONTHLY_AUDIT_SUMMARY");
        report.put("period", previousMonth.toString());
        report.put("periodStart", startOfMonth.toString());
        report.put("periodEnd", endOfMonth.toString());
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("statistics", stats);

        // Ensure output directory exists
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create report directory: " + outputDir);
        }

        // Write report to file
        String filename = String.format("audit-report-%s.json",
                previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        File reportFile = new File(dir, filename);

        ObjectMapper mapper = objectMapper.copy();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(reportFile, report);

        log.info("[AUDIT_REPORT] Report generated - file={}, totalActions={}",
                reportFile.getAbsolutePath(), stats.getTotalCount());

        return reportFile;
    }
}
