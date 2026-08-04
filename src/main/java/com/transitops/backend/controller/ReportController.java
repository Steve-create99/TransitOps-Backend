package com.transitops.backend.controller;

import com.transitops.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public Map<String, Object> report(@RequestParam(defaultValue = "weekly") String period) {
        return reportService.report(period);
    }

    @GetMapping("/export.csv")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public ResponseEntity<String> exportCsv(@RequestParam(defaultValue = "weekly") String period) {
        String csv = reportService.toCsv(period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transitops-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
