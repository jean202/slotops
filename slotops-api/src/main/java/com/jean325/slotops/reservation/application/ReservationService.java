package com.jean325.slotops.reservation.application;

import com.jean325.slotops.customer.domain.Customer;
import com.jean325.slotops.customer.domain.CustomerRepository;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.reservation.dto.PublicCreateReservationRequest;
import com.jean325.slotops.reservation.dto.PublicReservationResponse;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceRepository;
import com.jean325.slotops.resource.domain.ResourceStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import jakarta.persistence.EntityManager;
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

    public ReservationService(
            ResourceRepository resourceRepository,
            CustomerRepository customerRepository,
            ReservationRepository reservationRepository,
            ReservationPolicyValidator reservationPolicyValidator,
            ReservationCodeGenerator reservationCodeGenerator,
            EntityManager entityManager
    ) {
        this.resourceRepository = resourceRepository;
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
        this.reservationPolicyValidator = reservationPolicyValidator;
        this.reservationCodeGenerator = reservationCodeGenerator;
        this.entityManager = entityManager;
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
                ZoneId.of("Asia/Seoul").getRules().getOffset(request.date().atTime(request.startTime()))
        );
        OffsetDateTime endAt = startAt.plusMinutes(request.durationMinutes());

        reservationPolicyValidator.validate(resource, startAt, endAt, request.guestCount());

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
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                baseAmount,
                request.specialRequest()
        );

        Reservation saved = reservationRepository.save(reservation);
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
}
