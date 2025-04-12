package com.clinica.availability.timeslot.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class HistoricalSlotRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Busca o próximo slot disponível (considerando histórico)
     */
    public List<Map<String, Object>> findNextAvailableSlots(
            String resourceType, Long resourceId, LocalDateTime after, Long serviceTypeId) {

        // Construir a consulta SQL dinamicamente
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM time_slot WHERE resource_type = ? AND resource_id = ? ")
             .append("AND start_time > ? AND status = 'AVAILABLE' ");

        // Adicionar filtro por tipo de serviço se fornecido
        if (serviceTypeId != null) {
            query.append("AND service_type_id = ? ");
        }

        query.append("ORDER BY start_time LIMIT 10");

        // Executar a consulta
        List<Map<String, Object>> slots;
        if (serviceTypeId != null) {
            slots = jdbcTemplate.queryForList(query.toString(), resourceType, resourceId, after, serviceTypeId);
        } else {
            slots = jdbcTemplate.queryForList(query.toString(), resourceType, resourceId, after);
        }

        return slots;
    }

    /**
     * Busca um slot pelo ID (considerando histórico)
     */
    public Map<String, Object> findSlotById(Long slotId) {
        // Tentar buscar na tabela principal
        Map<String, Object> slot = findSlotInTable("time_slot", slotId);

        // Se não encontrar, tentar buscar nas tabelas de arquivo
        if (slot == null) {
            int year = LocalDateTime.now().getYear(); // Ano atual como padrão
            String archiveTable = "time_slot_archive_" + year;

            // Tentar buscar na tabela de arquivo do ano atual
            if (tableExists(archiveTable)) {
                slot = findSlotInTable(archiveTable, slotId);
            }

            // Se não encontrar, tentar buscar em tabelas de anos anteriores
            if (slot == null) {
                for (int i = year - 1; i >= 2020; i--) { // Limite de anos
                    archiveTable = "time_slot_archive_" + i;
                    if (tableExists(archiveTable)) {
                        slot = findSlotInTable(archiveTable, slotId);
                        if (slot != null) break; // Encontrou, sair do loop
                    }
                }
            }
        }

        return slot;
    }

    /**
     * Busca um slot em uma tabela específica
     */
    private Map<String, Object> findSlotInTable(String tableName, Long slotId) {
        try {
            return jdbcTemplate.queryForMap("SELECT * FROM " + tableName + " WHERE id = ?", slotId);
        } catch (Exception e) {
            return null; // Slot não encontrado ou erro
        }
    }

    /**
     * Atualiza o status de um slot (considerando histórico)
     */
    public boolean updateHistoricalSlotStatus(
            Long slotId, String newStatus, Long bookingId, String reason, String modifiedBy) {

        String tableName = determineSlotTableName(slotId);
        if (tableName == null) {
            return false; // Slot não encontrado
        }

        String sql = "UPDATE " + tableName + " SET status = ?, booking_id = ?, " +
                     "last_modified_by = ?, modification_reason = ?, updated_at = NOW() " +
                     "WHERE id = ?";

        int rowsAffected = jdbcTemplate.update(sql, newStatus, bookingId, modifiedBy, reason, slotId);
        return rowsAffected > 0;
    }

    /**
     * Determina em qual tabela o slot está armazenado
     */
    public String determineSlotTableName(Long slotId) {
        // Tentar encontrar na tabela principal
        if (slotExistsInTable("time_slot", slotId)) {
            return "time_slot";
        }

        // Se não encontrar, procurar nas tabelas de arquivo
        int year = LocalDateTime.now().getYear();
        for (int i = year; i >= 2020; i--) {
            String archiveTable = "time_slot_archive_" + i;
            if (tableExists(archiveTable) && slotExistsInTable(archiveTable, slotId)) {
                return archiveTable;
            }
        }

        return null; // Slot não encontrado em nenhuma tabela
    }

    /**
     * Verifica se um slot existe em uma tabela específica
     */
    private boolean slotExistsInTable(String tableName, Long slotId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT 1 FROM " + tableName + " WHERE id = ?", Integer.class, slotId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Registra modificação retroativa em log
     */
    public void logRetroactiveChange(
            Long slotId, String slotTable, String modifiedBy, String fromStatus, String toStatus,
            String reason, String resourceType, Long resourceId, Long tenantId, LocalDateTime slotDate) {

        String sql = "INSERT INTO retroactive_change_log (slot_id, slot_table, modified_by, " +
                     "from_status, to_status, reason, resource_type, resource_id, tenant_id, slot_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, slotId, slotTable, modifiedBy, fromStatus, toStatus, reason,
                            resourceType, resourceId, tenantId, slotDate.toLocalDate());
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