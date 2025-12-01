package com.kalabay.cloudstorage.folder.dto;

import com.kalabay.cloudstorage.folder.Folder;
import java.time.Instant;

public record FolderResponse(Long id, String name, Long parentId, Instant createdAt) {
    public static FolderResponse fromEntity(Folder folder) {
        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        return new FolderResponse(folder.getId(), folder.getName(), parentId, folder.getCreatedAt());
    }
}