package com.kalabay.cloudstorage.share.dto;

import jakarta.validation.constraints.Future;

import java.time.Instant;

public record CreateShareRequest(
        @Future(message = "{share.expiresAt.future}")
        Instant expiresAt
) {}
