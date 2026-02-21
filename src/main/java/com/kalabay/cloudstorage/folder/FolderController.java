package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.folder.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/**
 * REST controller for folder management.
 *
 * Provides endpoints to create folders, browse folder children, build a folder tree,
 * rename/move/delete folders, and resolve a folder path (breadcrumbs).
 *
 * All operations are executed in the scope of the authenticated user.
 */
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService service;

    public FolderController(FolderService service) {
        this.service = service;
    }

    /**
     * Creates a new folder for the authenticated user.
     *
     * @param request request body containing folder name and optional parentId
     * @param auth current authenticated user
     * @return created folder response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody CreateFolderRequest request, Authentication auth) {
        var folder = service.create(auth.getName(), request.name(), request.parentId());
        return FolderResponse.fromEntity(folder);
    }

    /**
     * Lists direct child folders for the authenticated user.
     *
     * If {@code parentId} is not provided, returns root-level folders.
     * Supports sorting via {@code sort} parameter in format {@code field,dir}
     * where {@code dir} is {@code asc} or {@code desc}.
     *
     * Supported sort fields:
     * - {@code createdAt}
     * - {@code name}
     *
     * @param parentId optional parent folder ID (null for root)
     * @param sort sorting expression (default: {@code createdAt,desc})
     * @param auth current authenticated user
     * @return sorted list of child folder responses
     * @throws IllegalArgumentException if sort field or direction is not supported
     */
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

    /**
     * Builds and returns the full folder tree for the authenticated user.
     *
     * @param auth current authenticated user
     * @return list of root nodes with nested children
     */
    @GetMapping("/tree")
    public List<FolderTreeNode> tree(Authentication auth) {
        return service.getTree(auth.getName());
    }

    /**
     * Renames a folder owned by the authenticated user.
     *
     * @param id folder ID
     * @param request request body containing new folder name
     * @param auth current authenticated user
     * @return updated folder response
     */
    @PatchMapping("/{id}")
    public FolderResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody RenameFolderRequest request,
            Authentication auth
    ) {
        var folder = service.rename(auth.getName(), id, request.name());
        return FolderResponse.fromEntity(folder);
    }

    /**
     * Moves a folder to a new parent folder (or to root if parentId is null).
     *
     * @param id folder ID
     * @param request request body containing destination parentId (nullable)
     * @param auth current authenticated user
     * @return updated folder response
     */
    @PatchMapping("/{id}/move")
    public FolderResponse move(
            @PathVariable Long id,
            @RequestBody MoveFolderRequest request,
            Authentication auth
    ) {
        var folder = service.move(auth.getName(), id, request.parentId());
        return FolderResponse.fromEntity(folder);
    }

    /**
     * Deletes a folder owned by the authenticated user.
     *
     * Deletion is performed recursively (subtree). Physical files belonging to the subtree
     * are removed from disk before database deletion.
     *
     * @param id folder ID
     * @param auth current authenticated user
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(auth.getName(), id);
    }

    /**
     * Returns a breadcrumb-style path from root to the specified folder.
     *
     * @param id folder ID
     * @param auth current authenticated user
     * @return ordered list of path items (root -> ... -> folder)
     */
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
