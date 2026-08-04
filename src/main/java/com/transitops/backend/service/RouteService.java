package com.transitops.backend.service;

import com.transitops.backend.dto.RouteDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.RouteEntity;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<RouteDtos.Response> list(String search, String status, Pageable pageable) {
        Page<RouteEntity> page;
        if (status != null && !status.isBlank()) {
            page = routeRepository.findByStatusIgnoreCase(status.trim(), pageable);
        } else if (search != null && !search.isBlank()) {
            String q = search.trim();
            page = routeRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(q, q, pageable);
        } else {
            page = routeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(RouteDtos.Response::from));
    }

    public RouteDtos.Response get(Long id) {
        return RouteDtos.Response.from(find(id));
    }

    @Transactional
    public RouteDtos.Response create(RouteDtos.Request req, String actor) {
        if (routeRepository.existsByCode(req.getCode())) {
            throw new ApiException("Route code already exists", HttpStatus.CONFLICT);
        }
        RouteEntity route = map(new RouteEntity(), req);
        routeRepository.save(route);
        auditService.log(actor, "CREATE", "Route", String.valueOf(route.getId()), route.getCode());
        return RouteDtos.Response.from(route);
    }

    @Transactional
    public RouteDtos.Response update(Long id, RouteDtos.Request req, String actor) {
        RouteEntity route = find(id);
        if (!route.getCode().equalsIgnoreCase(req.getCode()) && routeRepository.existsByCode(req.getCode())) {
            throw new ApiException("Route code already exists", HttpStatus.CONFLICT);
        }
        map(route, req);
        auditService.log(actor, "UPDATE", "Route", String.valueOf(id), route.getCode());
        return RouteDtos.Response.from(route);
    }

    @Transactional
    public void delete(Long id, String actor) {
        RouteEntity route = find(id);
        routeRepository.delete(route);
        auditService.log(actor, "DELETE", "Route", String.valueOf(id), route.getCode());
    }

    public RouteEntity find(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Route not found", HttpStatus.NOT_FOUND));
    }

    private RouteEntity map(RouteEntity route, RouteDtos.Request req) {
        route.setCode(req.getCode());
        route.setName(req.getName());
        route.setColor(req.getColor());
        route.setStartStop(req.getStartStop());
        route.setEndStop(req.getEndStop());
        route.setIntermediateStops(req.getIntermediateStops() != null ? req.getIntermediateStops() : new ArrayList<>());
        route.setStatus(req.getStatus() != null ? req.getStatus() : "Active");
        route.setFrequencyMinutes(req.getFrequencyMinutes() != null ? req.getFrequencyMinutes() : 15);
        route.setBusCount(req.getBusCount() != null ? req.getBusCount() : 0);
        route.setType(req.getType());
        route.setDirection(req.getDirection());
        return route;
    }
}
