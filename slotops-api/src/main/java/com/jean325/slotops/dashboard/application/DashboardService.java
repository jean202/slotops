package com.jean325.slotops.dashboard.application;

import com.jean325.slotops.dashboard.dto.DashboardSummaryResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.OrgAccessGuard;
import com.jean325.slotops.reservation.domain.ReservationRepository;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.reservation.domain.StatusCount;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    // Reservations that count as realized/expected revenue.
    private static final Set<ReservationStatus> REVENUE_STATUSES =
            EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);

    private final ReservationRepository reservationRepository;
    private final OrgAccessGuard orgAccessGuard;

    public DashboardService(ReservationRepository reservationRepository, OrgAccessGuard orgAccessGuard) {
        this.reservationRepository = reservationRepository;
        this.orgAccessGuard = orgAccessGuard;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(
            Long orgId, Long userId, OffsetDateTime from, OffsetDateTime to) {
        orgAccessGuard.requireMembership(orgId, userId);
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ApiException(
                    "INVALID_DATE_RANGE",
                    "The 'from' timestamp must be earlier than 'to'.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<StatusCount> counts = reservationRepository.countByStatus(orgId, from, to);

        // Report every status, defaulting to zero, in enum order for a stable shape.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReservationStatus status : ReservationStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        long total = 0;
        for (StatusCount count : counts) {
            byStatus.put(count.getStatus().name(), count.getCount());
            total += count.getCount();
        }

        BigDecimal revenue = reservationRepository.sumRevenue(orgId, REVENUE_STATUSES, from, to);

        return new DashboardSummaryResponse(total, byStatus, revenue);
    }
}
