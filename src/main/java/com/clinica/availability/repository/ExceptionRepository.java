package com.clinica.availability.repository;

import com.clinica.availability.model.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExceptionRepository extends JpaRepository<AvailabilityException, Long> {
}