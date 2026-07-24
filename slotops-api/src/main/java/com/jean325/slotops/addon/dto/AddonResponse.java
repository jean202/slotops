package com.jean325.slotops.addon.dto;

import java.math.BigDecimal;

public record AddonResponse(
        Long id,
        String name,
        BigDecimal price,
        String status
) {
}
