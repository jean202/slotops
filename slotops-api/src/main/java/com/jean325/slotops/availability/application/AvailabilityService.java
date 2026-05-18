package com.jean325.slotops.availability.application;

import com.jean325.slotops.availability.domain.AvailabilityRule;
import com.jean325.slotops.availability.domain.AvailabilityRuleRepository;
import com.jean325.slotops.availability.domain.BlackoutPeriod;
import com.jean325.slotops.availability.domain.BlackoutPeriodRepository;
import com.jean325.slotops.availability.domain.SlotCalculator;
import com.jean325.slotops.availability.dto.AvailabilityRuleResponse;
import com.jean325.slotops.availability.dto.AvailableSlotsResponse;
import com.jean325.slotops.availability.dto.BlackoutPeriodResponse;
import com.jean325.slotops.availability.dto.CreateAvailabilityRuleRequest;
import com.jean325.slotops.availability.dto.CreateBlackoutPeriodRequest;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceRepository;
import com.jean325.slotops.resource.domain.ResourceStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AvailabilityService {

    private final AvailabilityRuleRepository ruleRepository;
    private final BlackoutPeriodRepository blackoutRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final SlotCalculator slotCalculator;

    public AvailabilityService(
            AvailabilityRuleRepository ruleRepository,
            BlackoutPeriodRepository blackoutRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            SlotCalculator slotCalculator
    ) {
        this.ruleRepository = ruleRepository;
        this.blackoutRepository = blackoutRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.slotCalculator = slotCalculator;
    }

    // --- Availability Rules ---

    public AvailabilityRuleResponse createRule(Long orgId, Long resourceId, CreateAvailabilityRuleRequest request) {
        Resource resource = getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = new AvailabilityRule(
                resource,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.isOpen(),
                request.leadTimeMinutes(),
                request.sameDayCutoffTime()
        );
        return toRuleResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> listRules(Long orgId, Long resourceId) {
        getResourceForOrg(resourceId, orgId);
        return ruleRepository.findByResourceId(resourceId).stream()
                .map(this::toRuleResponse).toList();
    }

    public AvailabilityRuleResponse updateRule(Long orgId, Long resourceId, Long ruleId,
                                                CreateAvailabilityRuleRequest request) {
        getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("RULE_NOT_FOUND", "Availability rule not found", HttpStatus.NOT_FOUND));
        rule.setStartTime(request.startTime());
        rule.setEndTime(request.endTime());
        rule.setOpen(request.isOpen());
        rule.setLeadTimeMinutes(request.leadTimeMinutes());
        rule.setSameDayCutoffTime(request.sameDayCutoffTime());
        return toRuleResponse(ruleRepository.save(rule));
    }

    public void deleteRule(Long orgId, Long resourceId, Long ruleId) {
        getResourceForOrg(resourceId, orgId);
        AvailabilityRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("RULE_NOT_FOUND", "Availability rule not found", HttpStatus.NOT_FOUND));
        ruleRepository.delete(rule);
    }

    // --- Blackout Periods ---

    public BlackoutPeriodResponse createBlackout(Long orgId, Long resourceId, CreateBlackoutPeriodRequest request) {
        Resource resource = getResourceForOrg(resourceId, orgId);
        BlackoutPeriod period = new BlackoutPeriod(resource, request.startAt(), request.endAt(), request.reason());
        return toBlackoutResponse(blackoutRepository.save(period));
    }

    @Transactional(readOnly = true)
    public List<BlackoutPeriodResponse> listBlackouts(Long orgId, Long resourceId) {
        getResourceForOrg(resourceId, orgId);
        return blackoutRepository.findByResourceId(resourceId).stream()
                .map(this::toBlackoutResponse).toList();
    }

    public void deleteBlackout(Long orgId, Long resourceId, Long periodId) {
        getResourceForOrg(resourceId, orgId);
        BlackoutPeriod period = blackoutRepository.findById(periodId)
                .filter(p -> p.getResource().getId().equals(resourceId))
                .orElseThrow(() -> new ApiException("BLACKOUT_NOT_FOUND", "Blackout period not found", HttpStatus.NOT_FOUND));
        blackoutRepository.delete(period);
    }

    // --- Slot Query ---

    @Transactional(readOnly = true)
    public AvailableSlotsResponse getAvailableSlots(Long resourceId, LocalDate date, int durationMinutes) {
        Resource resource = resourceRepository.findByIdAndStatus(resourceId, ResourceStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND));

        if (durationMinutes % resource.getSlotUnitMinutes() != 0) {
            throw new ApiException("INVALID_DURATION",
                    "Duration must be a multiple of the resource's slot unit (" + resource.getSlotUnitMinutes() + " min)",
                    HttpStatus.BAD_REQUEST);
        }

        if (resource.getSlotUnitMinutes() == null) {
            throw new ApiException("RESOURCE_MISCONFIGURED",
                    "Resource slot unit is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (durationMinutes % resource.getSlotUnitMinutes() != 0) {
            throw new ApiException("INVALID_DURATION",
                    "Duration must be a multiple of the resource's slot unit (" + resource.getSlotUnitMinutes() + " min)",
                    HttpStatus.BAD_REQUEST);
        }

        ZoneId timezone = ZoneId.of(resource.getLocation().getTimezone());
        ZonedDateTime dayStart = date.atStartOfDay(timezone);
        ZonedDateTime dayEnd = date.plusDays(1).atStartOfDay(timezone);

        List<AvailabilityRule> rules = ruleRepository.findByResourceId(resourceId);
        List<BlackoutPeriod> blackouts = blackoutRepository.findOverlappingBetween(
                resourceId, dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime());
        List<Reservation> reservations = reservationRepository.findActiveForResourceBetween(
                resourceId, dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));

        List<SlotCalculator.AvailableSlot> slots = slotCalculator.calculate(
                resource, rules, blackouts, reservations, date, durationMinutes, timezone, OffsetDateTime.now());

        return new AvailableSlotsResponse(
                slots.stream().map(s -> new AvailableSlotsResponse.Slot(s.startAt(), s.endAt())).toList()
        );
    }

    // --- Availability Validation (called by ReservationService) ---

    @Transactional(readOnly = true)
    public void validateAvailability(Resource resource, OffsetDateTime startAt, OffsetDateTime endAt) {
        ZoneId timezone = ZoneId.of(resource.getLocation().getTimezone());
        LocalDate date = startAt.atZoneSameInstant(timezone).toLocalDate();
        int dayOfWeek = date.getDayOfWeek().getValue();

        AvailabilityRule rule = ruleRepository.findByResourceIdAndDayOfWeek(resource.getId(), dayOfWeek)
                .orElseThrow(() -> new ApiException(
                        "RESOURCE_UNAVAILABLE_ON_DAY",
                        "Resource is not available on this day of the week.",
                        HttpStatus.BAD_REQUEST));

        if (!rule.isOpen()) {
            throw new ApiException("RESOURCE_UNAVAILABLE_ON_DAY",
                    "Resource is not available on this day of the week.", HttpStatus.BAD_REQUEST);
        }

        LocalTime startLocal = startAt.atZoneSameInstant(timezone).toLocalTime();
        LocalTime endLocal = endAt.atZoneSameInstant(timezone).toLocalTime();
        if (startLocal.isBefore(rule.getStartTime()) || endLocal.isAfter(rule.getEndTime())) {
            throw new ApiException("RESERVATION_OUTSIDE_BUSINESS_HOURS",
                    "Reservation is outside the resource's business hours.", HttpStatus.BAD_REQUEST);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (startAt.minusMinutes(rule.getLeadTimeMinutes()).isBefore(now)) {
            throw new ApiException("RESERVATION_LEAD_TIME_VIOLATION",
                    "Reservation does not meet the required lead time.", HttpStatus.BAD_REQUEST);
        }

        if (date.equals(now.atZoneSameInstant(timezone).toLocalDate())
                && rule.getSameDayCutoffTime() != null
                && !now.atZoneSameInstant(timezone).toLocalTime().isBefore(rule.getSameDayCutoffTime())) {
            throw new ApiException("RESERVATION_PAST_SAME_DAY_CUTOFF",
                    "Same-day booking cutoff time has passed.", HttpStatus.BAD_REQUEST);
        }

        if (blackoutRepository.existsOverlap(resource.getId(), startAt, endAt)) {
            throw new ApiException("RESOURCE_BLACKED_OUT",
                    "Resource is blacked out during the requested time.", HttpStatus.BAD_REQUEST);
        }
    }

    private Resource getResourceForOrg(Long resourceId, Long orgId) {
        return resourceRepository.findById(resourceId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ApiException("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND));
    }

    private AvailabilityRuleResponse toRuleResponse(AvailabilityRule rule) {
        return new AvailabilityRuleResponse(
                rule.getId(), rule.getResource().getId(),
                rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime(),
                rule.isOpen(), rule.getLeadTimeMinutes(), rule.getSameDayCutoffTime()
        );
    }

    private BlackoutPeriodResponse toBlackoutResponse(BlackoutPeriod period) {
        return new BlackoutPeriodResponse(
                period.getId(), period.getResource().getId(),
                period.getStartAt(), period.getEndAt(), period.getReason()
        );
    }
}
