package com.kalabay.cloudstorage.dir;

import com.kalabay.cloudstorage.dir.dto.DirResponse;
import com.kalabay.cloudstorage.file.FileService;
import com.kalabay.cloudstorage.file.dto.FileResponse;
import com.kalabay.cloudstorage.folder.FolderService;
import com.kalabay.cloudstorage.folder.dto.FolderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/dir")
public class DirectoryController {

    private final FolderService folderService;
    private final FileService fileService;

    public DirectoryController(FolderService folderService, FileService fileService) {
        this.folderService = folderService;
        this.fileService = fileService;
    }

    /**
     * folderId = null -> root
     * sort: createdAt,desc (default)
     *
     * sort fields:
     * - createdAt -> folders by createdAt, files by uploadedAt
     * - name      -> folders by name, files by filename
     * - size      -> files by sizeBytes; folders fallback to name
     */
    @GetMapping
    public DirResponse dir(
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort,
            Authentication auth
    ) {
        String username = auth.getName();

        List<FolderResponse> folders = folderService.listChildren(username, folderId)
                .stream()
                .map(FolderResponse::fromEntity)
                .toList();

        List<FileResponse> files = fileService.list(username, folderId)
                .stream()
                .map(FileResponse::fromEntity)
                .toList();

        ParsedSort parsed = parseSort(sort);

        folders = sortFolders(folders, parsed);
        files = sortFiles(files, parsed);

        return new DirResponse(folderId, folders, files);
    }

    private List<FolderResponse> sortFolders(List<FolderResponse> items, ParsedSort sort) {
        Comparator<FolderResponse> cmp = switch (sort.field) {
            case "createdAt" -> Comparator.comparing(FolderResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "name" -> Comparator.comparing(FolderResponse::name, String.CASE_INSENSITIVE_ORDER);
            case "size" -> Comparator.comparing(FolderResponse::name, String.CASE_INSENSITIVE_ORDER); // fallback
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + sort.field);
        };

        if (!sort.asc) cmp = cmp.reversed();
        return items.stream().sorted(cmp).toList();
    }

    private List<FileResponse> sortFiles(List<FileResponse> items, ParsedSort sort) {
        Comparator<FileResponse> cmp = switch (sort.field) {
            case "createdAt" -> Comparator.comparing(FileResponse::uploadedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "name" -> Comparator.comparing(FileResponse::filename, String.CASE_INSENSITIVE_ORDER);
            case "size" -> Comparator.comparingLong(FileResponse::sizeBytes);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + sort.field);
        };

        if (!sort.asc) cmp = cmp.reversed();
        return items.stream().sorted(cmp).toList();
    }

    private ParsedSort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";

        boolean asc = switch (dir) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc|desc");
        };

        return new ParsedSort(field, asc);
    }

    private record ParsedSort(String field, boolean asc) {}
}
