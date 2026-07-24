package com.jean325.slotops.notification.application;

import com.jean325.slotops.customer.domain.Customer;
import com.jean325.slotops.notification.domain.NotificationChannel;
import com.jean325.slotops.notification.domain.NotificationLog;
import com.jean325.slotops.notification.domain.NotificationLogRepository;
import com.jean325.slotops.notification.domain.NotificationTemplate;
import com.jean325.slotops.reservation.domain.Reservation;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logs notification intent for reservation lifecycle events. No message is
 * actually sent yet; each event is recorded as a PENDING notification for a
 * future email/SMS provider to pick up.
 */
@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final Clock clock;

    public NotificationService(NotificationLogRepository notificationLogRepository, Clock clock) {
        this.notificationLogRepository = notificationLogRepository;
        this.clock = clock;
    }

    @Transactional
    public void logReservationEvent(Reservation reservation, NotificationTemplate template) {
        Customer customer = reservation.getCustomer();
        boolean hasEmail = customer.getEmail() != null && !customer.getEmail().isBlank();
        NotificationChannel channel = hasEmail ? NotificationChannel.EMAIL : NotificationChannel.SMS;
        String recipient = hasEmail ? customer.getEmail() : customer.getPhone();

        notificationLogRepository.save(new NotificationLog(
                reservation.getId(),
                customer.getId(),
                channel,
                template.name(),
                recipient,
                clock.instant()
        ));
    }
}
