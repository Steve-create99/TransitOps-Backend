package com.transitops.backend.controller;

import com.transitops.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public Map<String, Object> kpis() {
        return dashboardService.kpis();
    }

    @GetMapping("/charts")
    public Map<String, Object> charts() {
        return dashboardService.charts();
    }
}
