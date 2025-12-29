package com.kalabay.cloudstorage.share.dto;

import java.time.Instant;

public record CreateShareRequest(Instant expiresAt) {}