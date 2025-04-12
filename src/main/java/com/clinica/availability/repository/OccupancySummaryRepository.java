package com.clinica.availability.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OccupancySummaryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Marca um sumário histórico para recálculo
     */
    public void markSummaryForRecalculation(
            String resourceType, Long resourceId, Long tenantId, int year, int month) {

        jdbcTemplate.update(
            "INSERT INTO historical_occupancy_summary (resource_type, resource_id, tenant_id, year, month, needs_recalculation) " +
            "VALUES (?, ?, ?, ?, ?, 1) " +
            "ON DUPLICATE KEY UPDATE needs_recalculation = 1",
            resourceType, resourceId, tenantId, year, month
        );
    }

    /**
     * Recalcula um sumário histórico
     */
    public void recalculateHistoricalSummary(
            String resourceType, Long resourceId, Long tenantId, int year, int month) {

        // Lógica para recalcular o sumário (pode envolver queries complexas)
        // Exemplo simplificado:
        jdbcTemplate.update(
            "UPDATE historical_occupancy_summary SET total_slots = (SELECT COUNT(*) FROM time_slot " +
            "WHERE resource_type = ? AND resource_id = ? AND tenant_id = ? " +
            "AND YEAR(start_time) = ? AND MONTH(start_time) = ?) " +
            "WHERE resource_type = ? AND resource_id = ? AND tenant_id = ? " +
            "AND year = ? AND month = ?",
            resourceType, resourceId, tenantId, year, month,
            resourceType, resourceId, tenantId, year, month
        );

        // Limpar a flag de recálculo
        jdbcTemplate.update(
            "UPDATE historical_occupancy_summary SET needs_recalculation = 0 " +
            "WHERE resource_type = ? AND resource_id = ? AND tenant_id = ? " +
            "AND year = ? AND month = ?",
            resourceType, resourceId, tenantId, year, month
        );
    }
}