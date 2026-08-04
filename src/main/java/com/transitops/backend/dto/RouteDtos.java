package com.transitops.backend.dto;

import com.transitops.backend.entity.RouteEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public final class RouteDtos {
    private RouteDtos() {}

    @Data
    public static class Request {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        @NotBlank
        private String color;
        private String startStop;
        private String endStop;
        private List<String> intermediateStops;
        private String status;
        private Integer frequencyMinutes;
        private Integer busCount;
        private String type;
        private String direction;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String code;
        private String number;
        private String name;
        private String color;
        private String startStop;
        private String endStop;
        private List<String> intermediateStops;
        private String status;
        private Integer frequency;
        private Integer frequencyMinutes;
        private Integer buses;
        private Integer busCount;
        private String type;
        private String direction;

        public static Response from(RouteEntity r) {
            java.util.List<String> stops = r.getIntermediateStops() == null
                    ? java.util.List.of()
                    : new java.util.ArrayList<>(r.getIntermediateStops());
            return Response.builder()
                    .id(r.getId())
                    .code(r.getCode())
                    .number(r.getCode())
                    .name(r.getName())
                    .color(r.getColor())
                    .startStop(r.getStartStop())
                    .endStop(r.getEndStop())
                    .intermediateStops(stops)
                    .status(r.getStatus())
                    .frequency(r.getFrequencyMinutes())
                    .frequencyMinutes(r.getFrequencyMinutes())
                    .buses(r.getBusCount())
                    .busCount(r.getBusCount())
                    .type(r.getType())
                    .direction(r.getDirection())
                    .build();
        }
    }
}
