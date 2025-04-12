package com.clinica.availability.timeslot.dto;

import java.time.LocalDateTime;

/**
 * Classe auxiliar para representar um slot durante a geração
 */
class TimeSlotDTO {
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