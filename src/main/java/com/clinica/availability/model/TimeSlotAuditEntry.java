package com.clinica.availability.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_slot_audit")
public class TimeSlotAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long slotId;
    private LocalDateTime timestamp;
    private String action; // EX: "CREATE", "UPDATE", "DELETE"
    private String user;
    private String details; // JSON com detalhes da mudança

    // Getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}