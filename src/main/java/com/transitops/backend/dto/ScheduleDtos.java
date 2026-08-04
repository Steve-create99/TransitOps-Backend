package com.transitops.backend.dto;

import com.transitops.backend.entity.Schedule;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    @Data
    public static class Request {
        @NotNull
        private Long routeId;
        private LocalDate serviceDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private Boolean weekdays;
        private Boolean weekends;
        private Boolean holidays;
        private String delayStatus;
        private Integer delayMinutes;
        private Long driverId;
        private Long vehicleId;
        private String status;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private Long routeId;
        private String routeName;
        private String routeCode;
        private LocalDate serviceDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private boolean weekdays;
        private boolean weekends;
        private boolean holidays;
        private String delayStatus;
        private Integer delayMinutes;
        private Long driverId;
        private String driverName;
        private Long vehicleId;
        private String vehicleReg;
        private String status;

        public static Response from(Schedule s) {
            return Response.builder()
                    .id(s.getId())
                    .routeId(s.getRoute().getId())
                    .routeName(s.getRoute().getName())
                    .routeCode(s.getRoute().getCode())
                    .serviceDate(s.getServiceDate())
                    .departureTime(s.getDepartureTime())
                    .arrivalTime(s.getArrivalTime())
                    .weekdays(s.isWeekdays())
                    .weekends(s.isWeekends())
                    .holidays(s.isHolidays())
                    .delayStatus(s.getDelayStatus())
                    .delayMinutes(s.getDelayMinutes())
                    .driverId(s.getDriver() != null ? s.getDriver().getId() : null)
                    .driverName(s.getDriver() != null ? s.getDriver().getFirstName() + " " + s.getDriver().getLastName() : null)
                    .vehicleId(s.getVehicle() != null ? s.getVehicle().getId() : null)
                    .vehicleReg(s.getVehicle() != null ? s.getVehicle().getRegistrationNumber() : null)
                    .status(s.getStatus())
                    .build();
        }
    }
}
