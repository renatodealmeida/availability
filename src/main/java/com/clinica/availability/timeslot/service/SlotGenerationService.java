package com.clinica.availability.service;

import com.clinica.availability.model.AvailabilityRule;
import com.clinica.availability.repository.AvailabilityRuleRepository;
import com.clinica.availability.repository.ResourceAvailabilityRepository;
import com.clinica.availability.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SlotGenerationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AvailabilityRuleRepository ruleRepository;

    @Autowired
    private ResourceAvailabilityRepository resourceAvailabilityRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    /**
     * Gera slots para um recurso em um período específico
     */
    @Transactional
    public int generateSlotsForResource(
            String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate, Long tenantId) {

        // 1. Buscar padrões ativos para o recurso
        List<Map<String, Object>> patterns = resourceAvailabilityRepository
            .findActivePatternsByResource(resourceType, resourceId, startDate, endDate);

        int totalGenerated = 0;

        // 2. Para cada padrão, buscar regras e gerar slots
        for (Map<String, Object> pattern : patterns) {
            Long patternId = (Long) pattern.get("pattern_id");

            // 3. Buscar regras do padrão
            List<AvailabilityRule> rules = ruleRepository.findByPatternId(patternId);

            // 4. Gerar slots para o período usando as regras
            List<TimeSlotDTO> slots = generateSlotsFromRules(
                rules, resourceType, resourceId, startDate, endDate, tenantId);

            // 5. Buscar configurações de batch para slots em grupo
            List<Map<String, Object>> batchConfigs = getBatchConfigurations(patternId);

            // 6. Gerar slots de batch se houver configurações
            if (!batchConfigs.isEmpty()) {
                slots.addAll(generateBatchSlots(
                    batchConfigs, resourceType, resourceId, startDate, endDate, tenantId));
            }

            // 7. Salvar slots gerados em lote
            if (!slots.isEmpty()) {
                batchInsertSlots(slots);
                totalGenerated += slots.size();
            }
        }

        // 8. Aplicar exceções sobre os slots gerados
        applyExceptions(resourceType, resourceId, startDate, endDate);

        // 9. Gerar sumários de ocupação
        updateOccupancySummaries(resourceType, resourceId, startDate, endDate, tenantId);

        return totalGenerated;
    }

    /**
     * Regenera slots para um período (remove e recria)
     */
    @Transactional
    public int regenerateSlots(
            String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate, Long tenantId) {

        // 1. Remover slots existentes que não estão reservados
        jdbcTemplate.update(
            "DELETE FROM time_slot " +
            "WHERE resource_type = ? " +
            "AND resource_id = ? " +
            "AND DATE(start_time) BETWEEN ? AND ? " +
            "AND status IN ('AVAILABLE', 'BLOCKED')",
            resourceType, resourceId, startDate, endDate
        );

        // 2. Gerar novos slots
        return generateSlotsForResource(resourceType, resourceId, startDate, endDate, tenantId);
    }

    /**
     * Gera slots personalizados para um período
     */
    @Transactional
    public int generateCustomSlots(
            String resourceType, Long resourceId,
            LocalDateTime startDateTime, LocalDateTime endDateTime, Long tenantId) {

        // Implementação de geração de slots personalizados
        // Este método cria slots com base em parâmetros específicos, não em regras

        return 0; // Retorna número de slots gerados
    }

    /**
     * Gera slots com base nas regras de disponibilidade
     */
    private List<TimeSlotDTO> generateSlotsFromRules(
            List<AvailabilityRule> rules, String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate, Long tenantId) {

        List<TimeSlotDTO> generatedSlots = new ArrayList<>();

        // Para cada dia no período
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 0 = Domingo, 6 = Sábado
            int dayOfMonth = date.getDayOfMonth();

            // Para cada regra
            for (AvailabilityRule rule : rules) {
                // Verificar se a regra se aplica a este dia
                boolean applies = false;

                if (rule.getRuleType() == AvailabilityRule.RuleType.WEEKLY && rule.getWeekday() == dayOfWeek) {
                    applies = true;
                } else if (rule.getRuleType() == AvailabilityRule.RuleType.MONTHLY && rule.getDayOfMonth() == dayOfMonth) {
                    applies = true;
                } else if (rule.getRuleType() == AvailabilityRule.RuleType.CUSTOM &&
                           !date.isBefore(rule.getStartDate()) &&
                           (rule.getEndDate() == null || !date.isAfter(rule.getEndDate()))) {
                    applies = true;
                }

                if (applies) {
                    // Gerar slots para esta regra neste dia
                    LocalTime startTime = rule.getStartTime();
                    LocalTime endTime = rule.getEndTime();
                    int duration = rule.getSlotDuration();

                    while (startTime.plusMinutes(duration).compareTo(endTime) <= 0) {
                        LocalDateTime slotStart = LocalDateTime.of(date, startTime);
                        LocalDateTime slotEnd = slotStart.plusMinutes(duration);

                        // Gerar slot(s) - considerando paralelismo (max_slots)
                        for (int i = 0; i < rule.getMaxSlots(); i++) {
                            TimeSlotDTO slot = new TimeSlotDTO();
                            slot.setResourceType(resourceType);
                            slot.setResourceId(resourceId);
                            slot.setStartTime(slotStart);
                            slot.setEndTime(slotEnd);
                            slot.setStatus("AVAILABLE");
                            slot.setTenantId(tenantId);

                            generatedSlots.add(slot);
                        }

                        startTime = startTime.plusMinutes(duration);
                    }
                }
            }
        }

        return generatedSlots;
    }

    /**
     * Busca configurações de batch para um padrão
     */
    private List<Map<String, Object>> getBatchConfigurations(Long patternId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM slot_batch_config WHERE pattern_id = ?", patternId);
    }

    /**
     * Gera slots em lote baseados em configurações de batch
     */
    private List<TimeSlotDTO> generateBatchSlots(
            List<Map<String, Object>> batchConfigs, String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate, Long tenantId) {

        List<TimeSlotDTO> batchSlots = new ArrayList<>();

        // Para cada configuração de batch
        for (Map<String, Object> config : batchConfigs) {
            Integer weekday = (Integer) config.get("weekday");
            Integer totalSlots = (Integer) config.get("total_slots");
            Integer parallelCapacity = (Integer) config.get("parallel_capacity");
            Integer avgDuration = (Integer) config.get("avg_duration");
            LocalTime startTime = ((java.sql.Time) config.get("start_time")).toLocalTime();
            LocalTime endTime = ((java.sql.Time) config.get("end_time")).toLocalTime();

            // Calcular número de linhas (rows)
            int rows = (int) Math.ceil((double) totalSlots / parallelCapacity);

            // Para cada dia no período
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // Verificar se é o dia da semana correto
                if (date.getDayOfWeek().getValue() % 7 == weekday) {
                    // Calcular duração total disponível em minutos
                    long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

                    // Calcular intervalo entre slots
                    long slotInterval = Math.max(totalMinutes / rows, avgDuration);

                    // Gerar slots para cada linha
                    for (int row = 0; row < rows; row++) {
                        // Calcular horário de início para esta linha
                        LocalTime rowStartTime = startTime.plusMinutes(row * slotInterval);
                        if (rowStartTime.compareTo(endTime) >= 0) {
                            break; // Passou do horário final
                        }

                        // Calcular horário de fim para esta linha
                        LocalTime rowEndTime = rowStartTime.plusMinutes(avgDuration);
                        if (rowEndTime.compareTo(endTime) > 0) {
                            rowEndTime = endTime;
                        }

                        // Definir horários de início e fim do slot
                        LocalDateTime slotStart = LocalDateTime.of(date, rowStartTime);
                        LocalDateTime slotEnd = LocalDateTime.of(date, rowEndTime);

                        // Gerar slots paralelos para esta linha
                        int slotsInThisRow = Math.min(parallelCapacity,
                                                     totalSlots - row * parallelCapacity);

                        for (int pos = 0; pos < slotsInThisRow; pos++) {
                            TimeSlotDTO slot = new TimeSlotDTO();
                            slot.setResourceType(resourceType);
                            slot.setResourceId(resourceId);
                            slot.setStartTime(slotStart);
                            slot.setEndTime(slotEnd);
                            slot.setStatus("AVAILABLE");
                            slot.setTenantId(tenantId);
                            slot.setBatchRow(row + 1);
                            slot.setBatchPosition(pos + 1);

                            batchSlots.add(slot);
                        }
                    }
                }
            }
        }

        return batchSlots;
    }

    /**
     * Insere slots em lote no banco de dados
     */
    private void batchInsertSlots(List<TimeSlotDTO> slots) {
        final int batchSize = 1000;

        for (int i = 0; i < slots.size(); i += batchSize) {
            final List<TimeSlotDTO> batch =
                slots.subList(i, Math.min(i + batchSize, slots.size()));

            jdbcTemplate.batchUpdate(
                "INSERT INTO time_slot " +
                "(resource_type, resource_id, start_time, end_time, " +
                "status, tenant_id, batch_row, batch_position) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int j) throws SQLException {
                        TimeSlotDTO slot = batch.get(j);
                        ps.setString(1, slot.getResourceType());
                        ps.setLong(2, slot.getResourceId());
                        ps.setTimestamp(3, Timestamp.valueOf(slot.getStartTime()));
                        ps.setTimestamp(4, Timestamp.valueOf(slot.getEndTime()));
                        ps.setString(5, slot.getStatus());
                        ps.setLong(6, slot.getTenantId());
                        ps.setObject(7, slot.getBatchRow());
                        ps.setObject(8, slot.getBatchPosition());
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                }
            );
        }
    }

    /**
     * Aplica exceções aos slots gerados
     */
    private void applyExceptions(
            String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Buscar exceções para o período
        List<Map<String, Object>> exceptions = jdbcTemplate.queryForList(
            "SELECT * FROM availability_exception " +
            "WHERE resource_type = ? AND resource_id = ? " +
            "AND start_datetime < ? AND end_datetime > ?",
            resourceType, resourceId, endDateTime, startDateTime
        );

        // Aplicar cada exceção
        for (Map<String, Object> exception : exceptions) {
            String exceptionType = (String) exception.get("exception_type");
            LocalDateTime exStart = ((Timestamp) exception.get("start_datetime")).toLocalDateTime();
            LocalDateTime exEnd = ((Timestamp) exception.get("end_datetime")).toLocalDateTime();
            String reason = (String) exception.get("reason");

            if ("BLOCK".equals(exceptionType)) {
                // Bloquear slots no período
                jdbcTemplate.update(
                    "UPDATE time_slot " +
                    "SET status = 'BLOCKED', " +
                    "    blocking_reason = ? " +
                    "WHERE resource_type = ? AND resource_id = ? " +
                    "AND start_time >= ? AND end_time <= ? " +
                    "AND status = 'AVAILABLE'",
                    reason, resourceType, resourceId, exStart, exEnd
                );
            }
        }
    }

    /**
     * Atualiza sumários de ocupação para slots gerados
     */
    private void updateOccupancySummaries(
            String resourceType, Long resourceId,
            LocalDate startDate, LocalDate endDate, Long tenantId) {

        jdbcTemplate.update(
            "REPLACE INTO occupancy_summary " +
            "(resource_type, resource_id, date, hour, " +
            "total_slots, available_slots, booked_slots, blocked_slots, " +
            "completed_slots, tenant_id) " +
            "SELECT " +
            "    resource_type, " +
            "    resource_id, " +
            "    DATE(start_time) as date, " +
            "    HOUR(start_time) as hour, " +
            "    COUNT(*) as total_slots, " +
            "    SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) as available_slots, " +
            "    SUM(CASE WHEN status = 'BOOKED' THEN 1 ELSE 0 END) as booked_slots, " +
            "    SUM(CASE WHEN status = 'BLOCKED' THEN 1 ELSE 0 END) as blocked_slots, " +
            "    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_slots, " +
            "    tenant_id " +
            "FROM time_slot " +
            "WHERE resource_type = ? AND resource_id = ? " +
            "AND DATE(start_time) BETWEEN ? AND ? " +
            "AND tenant_id = ? " +
            "GROUP BY resource_type, resource_id, DATE(start_time), HOUR(start_time), tenant_id",
            resourceType, resourceId, startDate, endDate, tenantId
        );
    }

    /**
     * Classe auxiliar para representar um slot durante a geração
     */
    private static class TimeSlotDTO {
        private String resourceType;
        private Long resourceId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private Long tenantId;
        private Integer batchRow;
        private Integer batchPosition;

        // Getters e setters
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }

        public Long getResourceId() { return resourceId; }
        public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

        public Integer getBatchRow() { return batchRow; }
        public void setBatchRow(Integer batchRow) { this.batchRow = batchRow; }

        public Integer getBatchPosition() { return batchPosition; }
        public void setBatchPosition(Integer batchPosition) { this.batchPosition = batchPosition; }
    }
}