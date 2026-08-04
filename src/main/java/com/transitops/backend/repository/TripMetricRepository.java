package com.transitops.backend.repository;

import com.transitops.backend.entity.TripMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TripMetricRepository extends JpaRepository<TripMetric, Long> {
    List<TripMetric> findByMetricDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.passengers),0) FROM TripMetric t WHERE t.metricDate = :date")
    long sumPassengersForDate(LocalDate date);

    @Query("SELECT COALESCE(AVG(t.onTimePercentage),100) FROM TripMetric t WHERE t.metricDate BETWEEN :start AND :end")
    Double averageOnTime(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(AVG(t.averageSpeed),0) FROM TripMetric t WHERE t.metricDate BETWEEN :start AND :end")
    Double averageSpeed(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.completedTrips),0) FROM TripMetric t WHERE t.metricDate = :date")
    long sumCompletedTrips(LocalDate date);
}
