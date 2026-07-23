package com.jean325.slotops.global.config;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope. Keeps Spring Data's {@link Page} internals
 * (pageable, sort descriptors) out of the public API shape.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
