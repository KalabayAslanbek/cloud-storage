package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.folder.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService service;

    public FolderController(FolderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody CreateFolderRequest request, Authentication auth) {
        var folder = service.create(auth.getName(), request.name(), request.parentId());
        return FolderResponse.fromEntity(folder);
    }

    @GetMapping
    public List<FolderResponse> listChildren(
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort,
            Authentication auth
    ) {
        var parsed = parseSort(sort);

        var list = service.listChildren(auth.getName(), parentId)
                .stream()
                .map(FolderResponse::fromEntity)
                .toList();

        return sortFolders(list, parsed);
    }

    @GetMapping("/tree")
    public List<FolderTreeNode> tree(Authentication auth) {
        return service.getTree(auth.getName());
    }

    @PatchMapping("/{id}")
    public FolderResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody RenameFolderRequest request,
            Authentication auth
    ) {
        var folder = service.rename(auth.getName(), id, request.name());
        return FolderResponse.fromEntity(folder);
    }

    @PatchMapping("/{id}/move")
    public FolderResponse move(
            @PathVariable Long id,
            @RequestBody MoveFolderRequest request,
            Authentication auth
    ) {
        var folder = service.move(auth.getName(), id, request.parentId());
        return FolderResponse.fromEntity(folder);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(auth.getName(), id);
    }

    @GetMapping("/{id}/path")
    public List<FolderPathItem> path(@PathVariable Long id, Authentication auth) {
        return service.getPath(auth.getName(), id);
    }

    private List<FolderResponse> sortFolders(List<FolderResponse> items, ParsedSort sort) {
        Comparator<FolderResponse> cmp = switch (sort.field) {
            case "createdAt" -> Comparator.comparing(FolderResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "name" -> Comparator.comparing(FolderResponse::name, String.CASE_INSENSITIVE_ORDER);
            default -> throw new IllegalArgumentException("Unsupported sort field: " + sort.field);
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
            default -> throw new IllegalArgumentException("Sort direction must be asc|desc");
        };

        return new ParsedSort(field, asc);
    }

    private record ParsedSort(String field, boolean asc) {}
}
