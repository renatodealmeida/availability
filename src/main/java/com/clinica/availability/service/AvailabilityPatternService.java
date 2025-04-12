package com.clinica.availability.service;

import com.clinica.availability.model.AvailabilityPattern;
import com.clinica.availability.model.AvailabilityRule;
import com.clinica.availability.repository.AvailabilityPatternRepository;
import com.clinica.availability.repository.AvailabilityRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AvailabilityPatternService {

    @Autowired
    private AvailabilityPatternRepository patternRepository;

    @Autowired
    private AvailabilityRuleRepository ruleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Cria um novo padrão de disponibilidade
     */
    @Transactional
    public AvailabilityPattern createPattern(String name, Long tenantId, String createdBy) {
        AvailabilityPattern pattern = new AvailabilityPattern();
        pattern.setName(name);
        pattern.setTenantId(tenantId);
        pattern.setCreatedBy(createdBy);
        pattern.setActive(true);

        return patternRepository.save(pattern);
    }

    /**
     * Adiciona uma regra semanal a um padrão
     */
    @Transactional
    public AvailabilityRule addWeeklyRule(
            Long patternId, Integer weekday, LocalTime startTime,
            LocalTime endTime, Integer slotDuration, Integer maxSlots) {

        String result = validateAndInsertRule(patternId, "WEEKLY",
                weekday, null, null, null, startTime, endTime,
                slotDuration, maxSlots);

        if ("CONFLICT".equals(result)) {
            throw new IllegalArgumentException("Rule conflicts with existing rules");
        }

        AvailabilityRule rule = new AvailabilityRule();
        rule.setPatternId(patternId);
        rule.setRuleType(AvailabilityRule.RuleType.WEEKLY);
        rule.setWeekday(weekday);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setSlotDuration(slotDuration);
        rule.setMaxSlots(maxSlots);

        return ruleRepository.save(rule);
    }

    /**
     * Adiciona uma regra personalizada a um padrão
     */
    @Transactional
    public AvailabilityRule addCustomRule(
            Long patternId, LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime,
            Integer slotDuration, Integer maxSlots) {

        String result = validateAndInsertRule(patternId, "CUSTOM",
                null, null, startDate, endDate, startTime, endTime,
                slotDuration, maxSlots);

        if ("CONFLICT".equals(result)) {
            throw new IllegalArgumentException("Rule conflicts with existing rules");
        }

        AvailabilityRule rule = new AvailabilityRule();
        rule.setPatternId(patternId);
        rule.setRuleType(AvailabilityRule.RuleType.CUSTOM);
        rule.setStartDate(startDate);
        rule.setEndDate(endDate);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setSlotDuration(slotDuration);
        rule.setMaxSlots(maxSlots);

        return ruleRepository.save(rule);
    }

    /**
     * Associa um padrão a um recurso
     */
    @Transactional
    public void associatePatternToResource(
            String resourceType, Long resourceId, Long patternId,
            LocalDate startDate, LocalDate endDate, Long tenantId, String createdBy) {

        jdbcTemplate.update(
            "INSERT INTO resource_availability " +
            "(resource_type, resource_id, pattern_id, start_date, end_date, tenant_id, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            resourceType, resourceId, patternId, startDate, endDate, tenantId, createdBy
        );
    }

    /**
     * Configura vagas em grupo para um padrão
     */
    @Transactional
    public void configureBatchSlots(
            Long patternId, Integer weekday, LocalTime startTime, LocalTime endTime,
            Integer totalSlots, Integer parallelCapacity, Integer avgDuration, Long tenantId) {

        jdbcTemplate.update(
            "INSERT INTO slot_batch_config " +
            "(pattern_id, weekday, start_time, end_time, total_slots, " +
            "parallel_capacity, avg_duration, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            patternId, weekday, startTime, endTime, totalSlots,
            parallelCapacity, avgDuration, tenantId
        );
    }

    /**
     * Valida e insere uma regra usando procedure de banco de dados
     */
    private String validateAndInsertRule(
            Long patternId, String ruleType, Integer weekday, Integer dayOfMonth,
            LocalDate startDate, LocalDate endDate, LocalTime startTime,
            LocalTime endTime, Integer slotDuration, Integer maxSlots) {

        return jdbcTemplate.queryForObject(
            "CALL ValidateAndInsertAvailabilityRule(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, @result); " +
            "SELECT @result;",
            String.class,
            patternId, ruleType, weekday, dayOfMonth, startDate, endDate,
            startTime, endTime, slotDuration, maxSlots
        );
    }
}