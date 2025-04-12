package com.clinica.availability.repository;

import com.clinica.availability.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    List<AvailabilityException> findByResourceTypeAndResourceIdAndEndDatetimeGreaterThanAndStartDatetimeLessThan(
            String resourceType, Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}