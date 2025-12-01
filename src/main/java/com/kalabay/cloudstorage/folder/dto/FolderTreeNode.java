package com.kalabay.cloudstorage.folder.dto;

import java.time.Instant;
import java.util.List;

public record FolderTreeNode( Long id, String name, Long parentId, Instant createdAt, List<FolderTreeNode> children) {}