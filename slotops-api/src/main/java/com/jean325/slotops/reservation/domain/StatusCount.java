package com.jean325.slotops.reservation.domain;

/** Projection for a reservation-count-per-status aggregate query. */
public interface StatusCount {
    ReservationStatus getStatus();

    long getCount();
}
