package com.jean325.slotops.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReservationCodeGeneratorTest {

    private final ReservationCodeGenerator generator = new ReservationCodeGenerator();

    @Test
    void codeEmbedsTheReservationDate() {
        assertThat(generator.generate(LocalDate.of(2026, 6, 5))).matches("RSV-20260605-[0-9A-F]{4}");
    }

    @Test
    void codesForTheSameDateVary() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            codes.add(generator.generate(LocalDate.of(2026, 6, 5)));
        }

        // 4 hex chars = 65,536 values; 50 draws collapsing to a handful would mean it is not random.
        assertThat(codes.size()).isGreaterThan(40);
    }
}
