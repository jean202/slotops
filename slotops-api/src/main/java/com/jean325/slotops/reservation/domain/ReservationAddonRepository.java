package com.jean325.slotops.reservation.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationAddonRepository extends JpaRepository<ReservationAddon, Long> {

    List<ReservationAddon> findByReservationId(Long reservationId);
}
