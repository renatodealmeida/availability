package com.clinica.availability.service;

import com.clinica.availability.repository.OccupancySummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class SummaryReconciliationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OccupancySummaryRepository summaryRepository;

    /**
     * Job que recalcula sumários marcados para atualização
     */
    @Scheduled(fixedRate = 900000) // 15 minutos
    @Transactional
    public void recalculateMarkedSummaries() {
        // 1. Buscar sumários históricos marcados para recálculo
        List<Map<String, Object>> summaries = jdbcTemplate.queryForList(
            "SELECT resource_type, resource_id, tenant_id, year, month " +
            "FROM historical_occupancy_summary " +
            "WHERE needs_recalculation = 1 " +
            "LIMIT 100"
        );

        // 2. Recalcular cada sumário
        for (Map<String, Object> summary : summaries) {
            String resourceType = (String) summary.get("resource_type");
            Long resourceId = (Long) summary.get("resource_id");
            Long tenantId = (Long) summary.get("tenant_id");
            int year = (int) summary.get("year");
            int month = (int) summary.get("month");

            summaryRepository.recalculateHistoricalSummary(
                resourceType, resourceId, tenantId, year, month);
        }

        // 3. Buscar sumários diários marcados para recálculo
        List<Map<String, Object>> dailySummaries = jdbcTemplate.queryForList(
            "SELECT resource_type, resource_id, tenant_id, date " +
            "FROM occupancy_summary " +
            "WHERE needs_recalculation = 1 " +
            "GROUP BY resource_type, resource_id, tenant_id, date " +
            "LIMIT 100"
        );

        // 4. Recalcular sumários diários
        for (Map<String, Object> summary : dailySummaries) {
            String resourceType = (String) summary.get("resource_type");
            Long resourceId = (Long) summary.get("resource_id");
            Long tenantId = (Long) summary.get("tenant_id");
            LocalDate date = ((java.sql.Date) summary.get("date")).toLocalDate();

            recalculateDailySummary(resourceType, resourceId, tenantId, date);
        }
    }

    /**
     * Recalcula sumário diário de ocupação
     */
    @Transactional
    public void recalculateDailySummary(
            String resourceType, Long resourceId, Long tenantId, LocalDate date) {

        // Construir consulta para dados dos slots (incluindo arquivados)
        StringBuilder query = new StringBuilder();
        query.append("REPLACE INTO occupancy_summary ")
             .append("(resource_type, resource_id, date, hour, ")
             .append("total_slots, available_slots, booked_slots, blocked_slots, ")
             .append("completed_slots, tenant_id, needs_recalculation) ")
             .append("SELECT ")
             .append("    ?, ?, ?, HOUR(start_time) as hour, ")
             .append("    COUNT(*) as total_slots, ")
             .append("    SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) as available_slots, ")
             .append("    SUM(CASE WHEN status = 'BOOKED' THEN 1 ELSE 0 END) as booked_slots, ")
             .append("    SUM(CASE WHEN status = 'BLOCKED' THEN 1 ELSE 0 END) as blocked_slots, ")
             .append("    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_slots, ")
             .append("    ?, 0 ")
             .append("FROM (");

        // Adicionar consulta para tabela principal
        query.append("SELECT * FROM time_slot WHERE resource_type = ? AND resource_id = ? ")
             .append("AND tenant_id = ? AND DATE(start_time) = ? ");

        // Verificar e adicionar tabela de arquivo do ano correspondente
        String archiveTable = "time_slot_archive_" + date.getYear();
        if (tableExists(archiveTable)) {
            query.append("UNION ALL ")
                 .append("SELECT * FROM ").append(archiveTable)
                 .append(" WHERE resource_type = ? AND resource_id = ? ")
                 .append("AND tenant_id = ? AND DATE(start_time) = ? ");
        }

        query.append(") as combined_slots ")
             .append("GROUP BY HOUR(start_time)");

        // Executar a consulta com parâmetros
        if (tableExists(archiveTable)) {
            jdbcTemplate.update(
                query.toString(),
                resourceType, resourceId, date, tenantId,
                resourceType, resourceId, tenantId, date,
                resourceType, resourceId, tenantId, date
            );
        } else {
            jdbcTemplate.update(
                query.toString(),
                resourceType, resourceId, date, tenantId,
                resourceType, resourceId, tenantId, date
            );
        }
    }

    /**
     * Verifica se uma tabela existe no banco de dados
     */
    private boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}