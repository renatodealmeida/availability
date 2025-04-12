package com.clinica.availability.timeslot.repository;

import com.clinica.availability.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByResourceTypeAndResourceIdAndStartTimeBetweenAndStatus(
            String resourceType, Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String status);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.resourceType = :resourceType " +
            "AND ts.resourceId = :resourceId AND ts.startTime > :startTime " +
            "AND ts.status = 'AVAILABLE' ORDER BY ts.startTime")
    List<TimeSlot> findNextAvailableSlots(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime);
}