package com.kalabay.cloudstorage.file.dto;

import com.kalabay.cloudstorage.file.StoredFile;
import java.time.Instant;

public record FileResponse(Long id, String filename, long sizeBytes, String contentType, Instant uploadedAt) {
    public static FileResponse fromEntity(StoredFile f) {
        return new FileResponse(f.getId(), f.getOriginalName(), f.getSizeBytes(), f.getContentType(), f.getUploadedAt());
    }
}