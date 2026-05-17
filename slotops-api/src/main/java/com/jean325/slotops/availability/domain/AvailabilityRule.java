package com.jean325.slotops.availability.domain;

import com.jean325.slotops.global.common.BaseEntity;
import com.jean325.slotops.resource.domain.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isOpen;
    private int leadTimeMinutes;
    private LocalTime sameDayCutoffTime;

    protected AvailabilityRule() {}

    public AvailabilityRule(Resource resource, int dayOfWeek, LocalTime startTime, LocalTime endTime,
                             boolean isOpen, int leadTimeMinutes, LocalTime sameDayCutoffTime) {
        this.resource = resource;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpen = isOpen;
        this.leadTimeMinutes = leadTimeMinutes;
        this.sameDayCutoffTime = sameDayCutoffTime;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isOpen() { return isOpen; }
    public int getLeadTimeMinutes() { return leadTimeMinutes; }
    public LocalTime getSameDayCutoffTime() { return sameDayCutoffTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setOpen(boolean open) { this.isOpen = open; }
    public void setLeadTimeMinutes(int leadTimeMinutes) { this.leadTimeMinutes = leadTimeMinutes; }
    public void setSameDayCutoffTime(LocalTime sameDayCutoffTime) { this.sameDayCutoffTime = sameDayCutoffTime; }
}
