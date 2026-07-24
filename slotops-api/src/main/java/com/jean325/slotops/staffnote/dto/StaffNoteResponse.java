package com.jean325.slotops.staffnote.dto;

import java.time.Instant;

public record StaffNoteResponse(
        Long id,
        Long authorUserId,
        String content,
        Instant createdAt
) {
}
