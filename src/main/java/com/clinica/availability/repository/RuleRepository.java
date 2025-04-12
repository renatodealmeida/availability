package com.clinica.availability.repository;

import com.clinica.availability.model.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<AvailabilityRule, Long> {
}