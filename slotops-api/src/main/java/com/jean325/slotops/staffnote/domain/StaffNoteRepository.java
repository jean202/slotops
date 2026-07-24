package com.jean325.slotops.staffnote.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffNoteRepository extends JpaRepository<StaffNote, Long> {

    List<StaffNote> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
