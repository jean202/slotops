package com.jean325.slotops.availability.api;

import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.availability.dto.AvailableSlotsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/resources")
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    public PublicAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{resourceId}/available-slots")
    public AvailableSlotsResponse getAvailableSlots(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int duration
    ) {
        return availabilityService.getAvailableSlots(resourceId, date, duration);
    }
}
