package com.clinica.availability.repository;

import com.clinica.availability.model.AvailabilityPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilityPatternRepository extends JpaRepository<AvailabilityPattern, Long> {
}