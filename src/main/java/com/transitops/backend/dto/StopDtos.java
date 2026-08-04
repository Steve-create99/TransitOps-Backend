package com.transitops.backend.dto;

import com.transitops.backend.entity.Stop;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

public final class StopDtos {
    private StopDtos() {}

    @Data
    public static class Request {
        @NotBlank
        private String name;
        private String zone;
        @NotNull
        private Double latitude;
        @NotNull
        private Double longitude;
        private Integer averageRiders;
        private Boolean wheelchairAccessible;
        private String amenities;
        private String status;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String zone;
        private Double latitude;
        private Double longitude;
        private Integer averageRiders;
        private boolean wheelchairAccessible;
        private String amenities;
        private String status;

        public static Response from(Stop s) {
            return Response.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .zone(s.getZone())
                    .latitude(s.getLatitude())
                    .longitude(s.getLongitude())
                    .averageRiders(s.getAverageRiders())
                    .wheelchairAccessible(s.isWheelchairAccessible())
                    .amenities(s.getAmenities())
                    .status(s.getStatus())
                    .build();
        }
    }
}
