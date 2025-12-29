package com.kalabay.cloudstorage.share.dto;

import com.kalabay.cloudstorage.share.FileShare;
import java.time.Instant;

public record ShareResponse (Long id, Long fileId, String token, Instant createdAt, Instant expiresAt, boolean revoked) {
    public static ShareResponse fromEntity(FileShare share) {
        return new ShareResponse(share.getId(), share.getFile().getId(), share.getToken(), share.getCreatedAt(), share.getExpiresAt(), share.isRevoked());
    }
}
