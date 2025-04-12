package com.clinica.availability.timeslot.service;

import com.clinica.availability.model.TimeSlot;
//import com.clinica.availability.model.TimeSlot.SlotStatus;
import com.clinica.availability.timeslot.repository.HistoricalSlotRepository;
import com.clinica.availability.timeSlot.repository.OccupancySummaryRepository;
import com.clinica.availability.timeSlot.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private HistoricalSlotRepository historicalSlotRepository;

    @Autowired
    private OccupancySummaryRepository occupancySummaryRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Busca slots disponíveis para um recurso
     */
    public List<TimeSlot> findAvailableSlots(
            String resourceType, Long resourceId,
            LocalDateTime startTime, LocalDateTime endTime) {

        return timeSlotRepository.findByResourceTypeAndResourceIdAndStartTimeBetweenAndStatus(
            resourceType, resourceId, startTime, endTime, TimeSlot.SlotStatus.AVAILABLE);
    }

    /**
     * Busca próximos slots disponíveis
     */
    public List<Map<String, Object>> findNextAvailableSlots(
            String resourceType, Long resourceId, LocalDateTime after, Long serviceTypeId) {

        String cacheKey = "nextSlots:" + resourceType + ":" + resourceId +
                         ":" + after.toLocalDate() + ":" + serviceTypeId;

        // Tentar recuperar do cache
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedSlots =
            (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedSlots != null) {
            return cachedSlots;
        }

        // Buscar do banco
        List<Map<String, Object>> slots = historicalSlotRepository.findNextAvailableSlots(
            resourceType, resourceId, after, serviceTypeId);

        // Armazenar no cache com TTL curto
        redisTemplate.opsForValue().set(cacheKey, slots, Duration.ofMinutes(5));

        return slots;
    }

    /**
     * Busca um slot por ID (atual ou arquivado)
     */
    public Map<String, Object> findSlotById(Long slotId) {
        String cacheKey = "slot:" + slotId;

        // Verificar cache
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedSlot =
            (Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedSlot != null) {
            return cachedSlot;
        }

        // Buscar usando repositório unificado
        Map<String, Object> slot = historicalSlotRepository.findSlotById(slotId);

        if (slot != null) {
            // Cache mais longo para slots arquivados
            boolean isArchived = (boolean) slot.getOrDefault("is_archived", false);
            Duration ttl = isArchived ? Duration.ofDays(1) : Duration.ofHours(1);
            redisTemplate.opsForValue().set(cacheKey, slot, ttl);
        }

        return slot;
    }

    /**
     * Reserva um slot para um agendamento
     */
    @Transactional
    public boolean bookSlot(
            Long slotId, Long bookingId, String modifiedBy, String reason) {

        // Buscar detalhes do slot
        Map<String, Object> slotData = findSlotById(slotId);
        if (slotData == null) {
            return false;
        }

        // Verificar se slot está disponível
        String currentStatus = (String) slotData.get("status");
        if (!"AVAILABLE".equals(currentStatus)) {
            return false;
        }

        // Atualizar o slot usando o repositório histórico
        boolean updated = historicalSlotRepository.updateHistoricalSlotStatus(
            slotId, "BOOKED", bookingId, reason, modifiedBy);

        if (updated) {
            // Registrar alteração para auditoria
            historicalSlotRepository.logRetroactiveChange(
                slotId,
                historicalSlotRepository.determineSlotTableName(slotId),
                modifiedBy,
                currentStatus,
                "BOOKED",
                reason,
                (String) slotData.get("resource_type"),
                (Long) slotData.get("resource_id"),
                (Long) slotData.get("tenant_id"),
                LocalDateTime.parse((String) slotData.get("start_time"))
            );

            // Marcar sumários para recálculo
            LocalDateTime startTime = LocalDateTime.parse((String) slotData.get("start_time"));
            occupancySummaryRepository.markSummaryForRecalculation(
                (String) slotData.get("resource_type"),
                (Long) slotData.get("resource_id"),
                (Long) slotData.get("tenant_id"),
                startTime.getYear(),
                startTime.getMonthValue()
            );

            // Invalidar cache
            invalidateSlotCache(slotId);
        }

        return updated;
    }

    /**
     * Atualiza o status de um slot (atual ou arquivado)
     */
    @Transactional
    public boolean updateSlotStatus(
            Long slotId, String newStatus, String reason, String modifiedBy) {

        // Buscar detalhes do slot
        Map<String, Object> slotData = findSlotById(slotId);
        if (slotData == null) {
            return false;
        }

        String currentStatus = (String) slotData.get("status");
        boolean updated = historicalSlotRepository.updateHistoricalSlotStatus(
            slotId, newStatus, null, reason, modifiedBy);

        if (updated) {
            // Registrar alteração para auditoria
            historicalSlotRepository.logRetroactiveChange(
                slotId,
                historicalSlotRepository.determineSlotTableName(slotId),
                modifiedBy,
                currentStatus,
                newStatus,
                reason,
                (String) slotData.get("resource_type"),
                (Long) slotData.get("resource_id"),
                (Long) slotData.get("tenant_id"),
                LocalDateTime.parse((String) slotData.get("start_time"))
            );

            // Marcar sumários para recálculo
            LocalDateTime startTime = LocalDateTime.parse((String) slotData.get("start_time"));
            occupancySummaryRepository.markSummaryForRecalculation(
                (String) slotData.get("resource_type"),
                (Long) slotData.get("resource_id"),
                (Long) slotData.get("tenant_id"),
                startTime.getYear(),
                startTime.getMonthValue()
            );

            // Invalidar cache
            invalidateSlotCache(slotId);
        }

        return updated;
    }

    /**
     * Invalida caches relacionados a um slot
     */
    private void invalidateSlotCache(Long slotId) {
        redisTemplate.delete("slot:" + slotId);
    }
}