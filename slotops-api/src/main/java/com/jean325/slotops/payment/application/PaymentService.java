package com.jean325.slotops.payment.application;

import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.organization.domain.OrganizationMemberRepository;
import com.jean325.slotops.payment.domain.Payment;
import com.jean325.slotops.payment.domain.PaymentRepository;
import com.jean325.slotops.payment.dto.PaymentResponse;
import com.jean325.slotops.payment.dto.UpdatePaymentRequest;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            Clock clock
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.clock = clock;
    }

    /** Called when a reservation is created; records an initial UNPAID payment. */
    @Transactional
    public void createForReservation(Reservation reservation) {
        paymentRepository.save(new Payment(reservation.getId()));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getForReservation(Long orgId, Long reservationId, Long userId) {
        requireOrgMembership(orgId, userId);
        getReservationForOrg(reservationId, orgId);
        return toResponse(getPayment(reservationId));
    }

    @Transactional
    public PaymentResponse updateStatus(Long orgId, Long reservationId, Long userId, UpdatePaymentRequest request) {
        requireOrgMembership(orgId, userId);
        Reservation reservation = getReservationForOrg(reservationId, orgId);
        Payment payment = getPayment(reservationId);
        payment.applyStatus(request.status(), reservation.getTotalAmount(), OffsetDateTime.now(clock), request.transactionRef());
        return toResponse(payment);
    }

    private Payment getPayment(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(
                        "PAYMENT_NOT_FOUND",
                        "No payment record exists for this reservation.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Reservation getReservationForOrg(Long reservationId, Long orgId) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new ApiException(
                        "RESERVATION_NOT_FOUND",
                        "The requested reservation was not found.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private void requireOrgMembership(Long orgId, Long userId) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
            throw new ApiException("FORBIDDEN",
                    "User is not a member of this organization", HttpStatus.FORBIDDEN);
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getPaymentStatus().name(),
                payment.getPaidAmount(),
                payment.getRefundedAmount(),
                payment.getTransactionRef(),
                payment.getPaidAt()
        );
    }
}
