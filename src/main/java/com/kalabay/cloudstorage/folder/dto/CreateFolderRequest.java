package com.kalabay.cloudstorage.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFolderRequest(
        @NotBlank(message = "{folder.name.notBlank}")
        @Size(min = 1, max = 255, message = "{folder.name.size}")
        @Pattern(
                regexp = "^(?!\\.{1,2}$)[^/\\\\]+$",
                message = "{folder.name.pattern}"
        )
        String name,
        Long parentId
) {}
