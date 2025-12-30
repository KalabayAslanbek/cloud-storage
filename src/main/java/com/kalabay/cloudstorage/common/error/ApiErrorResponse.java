package com.kalabay.cloudstorage.common.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> details
) {
    public record FieldErrorItem(String field, String message) {}
}
