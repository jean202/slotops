package com.jean325.slotops.reservation.application;

import com.jean325.slotops.addon.domain.Addon;
import com.jean325.slotops.addon.domain.AddonRepository;
import com.jean325.slotops.addon.domain.AddonStatus;
import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.customer.domain.Customer;
import com.jean325.slotops.customer.domain.CustomerRepository;
import com.jean325.slotops.global.config.PageResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.notification.application.NotificationService;
import com.jean325.slotops.notification.domain.NotificationTemplate;
import com.jean325.slotops.global.security.OrgAccessGuard;
import com.jean325.slotops.payment.application.PaymentService;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationAddon;
import com.jean325.slotops.reservation.domain.ReservationAddonRepository;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.reservation.dto.AdminReservationResponse;
import com.jean325.slotops.reservation.dto.PublicCreateReservationRequest;
import com.jean325.slotops.reservation.dto.PublicReservationResponse;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceRepository;
import com.jean325.slotops.resource.domain.ResourceStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ResourceRepository resourceRepository;
    private final CustomerRepository customerRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPolicyValidator reservationPolicyValidator;
    private final ReservationCodeGenerator reservationCodeGenerator;
    private final EntityManager entityManager;
    private final AvailabilityService availabilityService;
    private final OrgAccessGuard orgAccessGuard;
    private final PaymentService paymentService;
    private final AddonRepository addonRepository;
    private final ReservationAddonRepository reservationAddonRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    // Allowed source states keyed by the target state of a transition.
    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReservationStatus.CONFIRMED, EnumSet.of(ReservationStatus.PENDING),
            ReservationStatus.CANCELLED, EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
            ReservationStatus.COMPLETED, EnumSet.of(ReservationStatus.CONFIRMED),
            ReservationStatus.NO_SHOW, EnumSet.of(ReservationStatus.CONFIRMED)
    );

    public ReservationService(
            ResourceRepository resourceRepository,
            CustomerRepository customerRepository,
            ReservationRepository reservationRepository,
            ReservationPolicyValidator reservationPolicyValidator,
            ReservationCodeGenerator reservationCodeGenerator,
            EntityManager entityManager,
            AvailabilityService availabilityService,
            OrgAccessGuard orgAccessGuard,
            PaymentService paymentService,
            AddonRepository addonRepository,
            ReservationAddonRepository reservationAddonRepository,
            NotificationService notificationService,
            Clock clock
    ) {
        this.resourceRepository = resourceRepository;
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
        this.reservationPolicyValidator = reservationPolicyValidator;
        this.reservationCodeGenerator = reservationCodeGenerator;
        this.entityManager = entityManager;
        this.availabilityService = availabilityService;
        this.orgAccessGuard = orgAccessGuard;
        this.paymentService = paymentService;
        this.addonRepository = addonRepository;
        this.reservationAddonRepository = reservationAddonRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public PublicReservationResponse createPublicReservation(PublicCreateReservationRequest request) {
        Resource resource = resourceRepository.findByIdAndStatus(request.resourceId(), ResourceStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(
                        "RESOURCE_NOT_FOUND",
                        "The requested resource was not found.",
                        HttpStatus.NOT_FOUND
                ));

        OffsetDateTime startAt = OffsetDateTime.of(
                request.date(),
                request.startTime(),
                ZoneId.of(resource.getLocation().getTimezone()).getRules().getOffset(request.date().atTime(request.startTime()))
        );
        OffsetDateTime endAt = startAt.plusMinutes(request.durationMinutes());

        reservationPolicyValidator.validate(resource, startAt, endAt, request.guestCount());
        availabilityService.validateAvailability(resource, startAt, endAt);

        OffsetDateTime overlapStart = startAt.minusMinutes(resource.getBufferMinutes());
        OffsetDateTime overlapEnd = endAt.plusMinutes(resource.getBufferMinutes());
        boolean overlap = reservationRepository.existsOverlap(
                resource.getId(),
                overlapStart,
                overlapEnd,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );
        if (overlap) {
            throw new ApiException(
                    "RESERVATION_SLOT_UNAVAILABLE",
                    "The selected time slot is no longer available.",
                    HttpStatus.CONFLICT
            );
        }

        Customer customer = findOrCreateCustomer(resource, request);
        BigDecimal baseAmount = calculateBaseAmount(resource, request.durationMinutes());

        List<PublicCreateReservationRequest.AddonLine> addonLines =
                request.addons() == null ? List.of() : request.addons();
        Map<Long, Addon> addonsById = resolveAddons(resource.getOrganizationId(), addonLines);
        BigDecimal addonAmount = addonLines.stream()
                .map(line -> addonsById.get(line.addonId()).getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = baseAmount.add(addonAmount);

        Reservation reservation = new Reservation(
                resource.getOrganizationId(),
                resource.getLocationId(),
                resource,
                customer,
                reservationCodeGenerator.generate(request.date()),
                ReservationStatus.PENDING,
                startAt,
                endAt,
                request.guestCount(),
                baseAmount,
                addonAmount,
                BigDecimal.ZERO,
                totalAmount,
                request.specialRequest()
        );

        Reservation saved = reservationRepository.save(reservation);
        persistReservationAddons(saved.getId(), addonLines, addonsById);
        paymentService.createForReservation(saved);
        notificationService.logReservationEvent(saved, NotificationTemplate.RESERVATION_CREATED);
        return new PublicReservationResponse(
                saved.getId(),
                saved.getReservationCode(),
                saved.getStatus().name(),
                new PublicReservationResponse.LocationSummary(resource.getLocationId(), resource.getLocation().getName()),
                new PublicReservationResponse.ResourceSummary(resource.getId(), resource.getName()),
                new PublicReservationResponse.CustomerSummary(customer.getName(), customer.getPhone()),
                saved.getStartAt(),
                saved.getEndAt(),
                saved.getGuestCount(),
                new PublicReservationResponse.Pricing(
                        saved.getBaseAmount(),
                        saved.getAddonAmount(),
                        saved.getDiscountAmount(),
                        saved.getTotalAmount()
                ),
                "UNPAID"
        );
    }

    private Customer findOrCreateCustomer(Resource resource, PublicCreateReservationRequest request) {
        return customerRepository.findByOrganization_IdAndPhone(resource.getOrganizationId(), request.customer().phone())
                .map(customer -> {
                    customer.updateContact(request.customer().name(), request.customer().email(), request.customer().phone());
                    return customer;
                })
                .orElseGet(() -> customerRepository.save(new Customer(
                        entityManager.getReference(com.jean325.slotops.organization.domain.Organization.class, resource.getOrganizationId()),
                        request.customer().name(),
                        request.customer().email(),
                        request.customer().phone()
                )));
    }

    private BigDecimal calculateBaseAmount(Resource resource, Integer durationMinutes) {
        BigDecimal slots = BigDecimal.valueOf(durationMinutes)
                .divide(BigDecimal.valueOf(resource.getSlotUnitMinutes()));
        return resource.getBasePrice().multiply(slots);
    }

    /** Loads the requested addons and verifies each is an active addon of the org. */
    private Map<Long, Addon> resolveAddons(Long orgId, List<PublicCreateReservationRequest.AddonLine> addonLines) {
        if (addonLines.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = addonLines.stream()
                .map(PublicCreateReservationRequest.AddonLine::addonId)
                .distinct()
                .toList();
        Map<Long, Addon> addonsById = addonRepository
                .findByIdInAndOrganizationIdAndStatus(ids, orgId, AddonStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(Addon::getId, addon -> addon));
        if (addonsById.size() != ids.size()) {
            throw new ApiException(
                    "ADDON_UNAVAILABLE",
                    "One or more selected add-ons are unavailable.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return addonsById;
    }

    private void persistReservationAddons(
            Long reservationId,
            List<PublicCreateReservationRequest.AddonLine> addonLines,
            Map<Long, Addon> addonsById
    ) {
        if (addonLines.isEmpty()) {
            return;
        }
        List<ReservationAddon> rows = new ArrayList<>();
        for (PublicCreateReservationRequest.AddonLine line : addonLines) {
            Addon addon = addonsById.get(line.addonId());
            rows.add(new ReservationAddon(reservationId, addon.getId(), line.quantity(), addon.getPrice()));
        }
        reservationAddonRepository.saveAll(rows);
    }

    // --- Admin reservation workflow ---

    @Transactional
    public AdminReservationResponse confirm(Long orgId, Long reservationId, Long userId) {
        requireOrgMembership(orgId, userId);
        Reservation reservation = getForOrg(reservationId, orgId);
        checkTransition(reservation, ReservationStatus.CONFIRMED);
        reservation.changeStatus(ReservationStatus.CONFIRMED);
        notificationService.logReservationEvent(reservation, NotificationTemplate.RESERVATION_CONFIRMED);
        return toAdminResponse(reservation);
    }

    @Transactional
    public AdminReservationResponse cancel(Long orgId, Long reservationId, Long userId) {
        requireOrgMembership(orgId, userId);
        Reservation reservation = getForOrg(reservationId, orgId);
        checkTransition(reservation, ReservationStatus.CANCELLED);
        OffsetDateTime now = OffsetDateTime.now(clock);
        reservation.cancel(now);
        paymentService.applyCancellationRefund(reservation, now);
        notificationService.logReservationEvent(reservation, NotificationTemplate.RESERVATION_CANCELLED);
        return toAdminResponse(reservation);
    }

    @Transactional
    public AdminReservationResponse complete(Long orgId, Long reservationId, Long userId) {
        return transition(orgId, reservationId, userId, ReservationStatus.COMPLETED);
    }

    @Transactional
    public AdminReservationResponse markNoShow(Long orgId, Long reservationId, Long userId) {
        return transition(orgId, reservationId, userId, ReservationStatus.NO_SHOW);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReservationResponse> listForOrg(
            Long orgId,
            Long userId,
            ReservationStatus status,
            OffsetDateTime from,
            OffsetDateTime to,
            Long customerId,
            Pageable pageable
    ) {
        requireOrgMembership(orgId, userId);
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ApiException(
                    "INVALID_DATE_RANGE",
                    "The 'from' timestamp must be earlier than 'to'.",
                    HttpStatus.BAD_REQUEST
            );
        }
        return PageResponse.from(
                reservationRepository.search(orgId, status, from, to, customerId, pageable)
                        .map(this::toAdminResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReservationResponse> listForCustomer(
            Long orgId,
            Long customerId,
            Long userId,
            Pageable pageable
    ) {
        requireOrgMembership(orgId, userId);
        if (!customerRepository.existsByIdAndOrganization_Id(customerId, orgId)) {
            throw new ApiException(
                    "CUSTOMER_NOT_FOUND",
                    "The requested customer was not found.",
                    HttpStatus.NOT_FOUND
            );
        }
        return PageResponse.from(
                reservationRepository.search(orgId, null, null, null, customerId, pageable)
                        .map(this::toAdminResponse)
        );
    }

    private AdminReservationResponse transition(Long orgId, Long reservationId, Long userId, ReservationStatus target) {
        requireOrgMembership(orgId, userId);
        Reservation reservation = getForOrg(reservationId, orgId);
        checkTransition(reservation, target);
        reservation.changeStatus(target);
        return toAdminResponse(reservation);
    }

    private void checkTransition(Reservation reservation, ReservationStatus target) {
        if (!ALLOWED_TRANSITIONS.get(target).contains(reservation.getStatus())) {
            throw new ApiException(
                    "INVALID_RESERVATION_STATE_TRANSITION",
                    "Cannot change reservation from " + reservation.getStatus() + " to " + target + ".",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void requireOrgMembership(Long orgId, Long userId) {
        orgAccessGuard.requireMembership(orgId, userId);
    }

    private Reservation getForOrg(Long reservationId, Long orgId) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ApiException(
                        "RESERVATION_NOT_FOUND",
                        "The requested reservation was not found.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private AdminReservationResponse toAdminResponse(Reservation reservation) {
        Resource resource = reservation.getResource();
        Customer customer = reservation.getCustomer();
        return new AdminReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                new AdminReservationResponse.ResourceSummary(resource.getId(), resource.getName()),
                new AdminReservationResponse.CustomerSummary(customer.getName(), customer.getPhone()),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getGuestCount(),
                reservation.getTotalAmount()
        );
    }
}
