package com.jean325.slotops.staffnote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffNoteRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
