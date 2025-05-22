package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "change_log_details")
public class ChangeLogDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "change_log_id", nullable = false)
    private ChangeLog changeLog;

    @Convert(converter = com.example.demo.converter.EntityTypeConverter.class)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "attribute_name", nullable = false)
    private String attributeName;

    @Column(name = "old_value", columnDefinition = "json")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "json")
    private String newValue;
    
    // Getter and Setter methods
    public ChangeLog getChangeLog() {
        return this.changeLog;
    }
    
    public void setChangeLog(ChangeLog changeLog) {
        this.changeLog = changeLog;
    }
    
    public EntityType getEntityType() {
        return this.entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return this.entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getAttributeName() {
        return this.attributeName;
    }
    
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }
    
    public String getOldValue() {
        return this.oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return this.newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
} 