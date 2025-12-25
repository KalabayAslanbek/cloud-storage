package com.kalabay.cloudstorage.dir.dto;

import com.kalabay.cloudstorage.file.dto.FileResponse;
import com.kalabay.cloudstorage.folder.dto.FolderResponse;
import java.util.List;

public record DirResponse(Long folderId, List<FolderResponse> folders, List<FileResponse> files) {}
