package com.transitops.backend.service;

import com.transitops.backend.dto.StopDtos;
import com.transitops.backend.dto.common.PageResponse;
import com.transitops.backend.entity.Stop;
import com.transitops.backend.exception.ApiException;
import com.transitops.backend.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StopService {

    private final StopRepository stopRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<StopDtos.Response> list(String search, String zone, Pageable pageable) {
        Page<Stop> page;
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasZone = zone != null && !zone.isBlank();
        if (hasSearch && hasZone) {
            page = stopRepository.findByNameContainingIgnoreCaseAndZoneIgnoreCase(search.trim(), zone.trim(), pageable);
        } else if (hasSearch) {
            page = stopRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else if (hasZone) {
            page = stopRepository.findByZoneIgnoreCase(zone.trim(), pageable);
        } else {
            page = stopRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(StopDtos.Response::from));
    }

    public StopDtos.Response get(Long id) {
        return StopDtos.Response.from(find(id));
    }

    @Transactional
    public StopDtos.Response create(StopDtos.Request req, String actor) {
        if (stopRepository.existsByName(req.getName())) {
            throw new ApiException("Stop name already exists", HttpStatus.CONFLICT);
        }
        Stop stop = map(new Stop(), req);
        stopRepository.save(stop);
        auditService.log(actor, "CREATE", "Stop", String.valueOf(stop.getId()), stop.getName());
        return StopDtos.Response.from(stop);
    }

    @Transactional
    public StopDtos.Response update(Long id, StopDtos.Request req, String actor) {
        Stop stop = find(id);
        if (!stop.getName().equalsIgnoreCase(req.getName()) && stopRepository.existsByName(req.getName())) {
            throw new ApiException("Stop name already exists", HttpStatus.CONFLICT);
        }
        map(stop, req);
        auditService.log(actor, "UPDATE", "Stop", String.valueOf(id), stop.getName());
        return StopDtos.Response.from(stop);
    }

    @Transactional
    public void delete(Long id, String actor) {
        Stop stop = find(id);
        stopRepository.delete(stop);
        auditService.log(actor, "DELETE", "Stop", String.valueOf(id), stop.getName());
    }

    private Stop find(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() -> new ApiException("Stop not found", HttpStatus.NOT_FOUND));
    }

    private Stop map(Stop stop, StopDtos.Request req) {
        stop.setName(req.getName());
        stop.setZone(req.getZone());
        stop.setLatitude(req.getLatitude());
        stop.setLongitude(req.getLongitude());
        stop.setAverageRiders(req.getAverageRiders() != null ? req.getAverageRiders() : 0);
        stop.setWheelchairAccessible(req.getWheelchairAccessible() == null || req.getWheelchairAccessible());
        stop.setAmenities(req.getAmenities());
        stop.setStatus(req.getStatus() != null ? req.getStatus() : "Active");
        return stop;
    }
}
