package com.jean325.slotops.reservation.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReservationCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public String generate(LocalDate date) {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase(Locale.ROOT);
        return "RSV-" + DATE_FORMATTER.format(date) + "-" + suffix;
    }
}
