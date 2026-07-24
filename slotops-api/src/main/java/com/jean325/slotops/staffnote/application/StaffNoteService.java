package com.jean325.slotops.staffnote.application;

import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.OrgAccessGuard;
import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.staffnote.domain.StaffNote;
import com.jean325.slotops.staffnote.domain.StaffNoteRepository;
import com.jean325.slotops.staffnote.dto.CreateStaffNoteRequest;
import com.jean325.slotops.staffnote.dto.StaffNoteResponse;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffNoteService {

    private final StaffNoteRepository staffNoteRepository;
    private final ReservationRepository reservationRepository;
    private final OrgAccessGuard orgAccessGuard;
    private final Clock clock;

    public StaffNoteService(
            StaffNoteRepository staffNoteRepository,
            ReservationRepository reservationRepository,
            OrgAccessGuard orgAccessGuard,
            Clock clock
    ) {
        this.staffNoteRepository = staffNoteRepository;
        this.reservationRepository = reservationRepository;
        this.orgAccessGuard = orgAccessGuard;
        this.clock = clock;
    }

    @Transactional
    public StaffNoteResponse add(Long orgId, Long reservationId, Long userId, CreateStaffNoteRequest request) {
        orgAccessGuard.requireMembership(orgId, userId);
        requireReservationInOrg(reservationId, orgId);
        StaffNote note = staffNoteRepository.save(
                new StaffNote(reservationId, userId, request.content(), clock.instant()));
        return toResponse(note);
    }

    @Transactional(readOnly = true)
    public List<StaffNoteResponse> list(Long orgId, Long reservationId, Long userId) {
        orgAccessGuard.requireMembership(orgId, userId);
        requireReservationInOrg(reservationId, orgId);
        return staffNoteRepository.findByReservationIdOrderByCreatedAtAsc(reservationId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireReservationInOrg(Long reservationId, Long orgId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(
                        "RESERVATION_NOT_FOUND",
                        "The requested reservation was not found.",
                        HttpStatus.NOT_FOUND
                ));
        if (!reservation.getOrganizationId().equals(orgId)) {
            throw new ApiException(
                    "RESERVATION_NOT_FOUND",
                    "The requested reservation was not found.",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    private StaffNoteResponse toResponse(StaffNote note) {
        return new StaffNoteResponse(
                note.getId(),
                note.getAuthorUserId(),
                note.getContent(),
                note.getCreatedAt()
        );
    }
}
