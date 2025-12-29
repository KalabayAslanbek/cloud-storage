package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.folder.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
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
    public FolderResponse create(@RequestBody CreateFolderRequest request, Authentication auth) {
        try {
            var folder = service.create(auth.getName(), request.name(), request.parentId());
            return FolderResponse.fromEntity(folder);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public List<FolderResponse> listChildren(@RequestParam(value = "parentId", required = false) Long parentId, @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort, Authentication auth) {
        try {
            var parsed = parseSort(sort);
            var list = service.listChildren(auth.getName(), parentId)
                    .stream()
                    .map(FolderResponse::fromEntity)
                    .toList();

            return sortFolders(list, parsed);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/tree")
    public List<FolderTreeNode> tree(Authentication auth) {
        return service.getTree(auth.getName());
    }

    @PatchMapping("/{id}")
    public FolderResponse rename(@PathVariable Long id, @RequestBody RenameFolderRequest request, Authentication auth) {
        try {
            var folder = service.rename(auth.getName(), id, request.name());
            return FolderResponse.fromEntity(folder);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/{id}/move")
    public FolderResponse move(@PathVariable Long id, @RequestBody MoveFolderRequest request, Authentication auth) {
        try {
            var folder = service.move(auth.getName(), id, request.parentId());
            return FolderResponse.fromEntity(folder);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        try {
            service.delete(auth.getName(), id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/{id}/path")
    public List<FolderPathItem> path(@PathVariable Long id, Authentication auth) {
        try {
            return service.getPath(auth.getName(), id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private List<FolderResponse> sortFolders(List<FolderResponse> items, ParsedSort sort) {
        java.util.Comparator<FolderResponse> cmp = switch (sort.field) {
            case "createdAt" -> java.util.Comparator.comparing(FolderResponse::createdAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "name" -> java.util.Comparator.comparing(FolderResponse::name, String.CASE_INSENSITIVE_ORDER);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + sort.field);
        };

        if (!sort.asc) cmp = cmp.reversed();
        return items.stream()
                .sorted(cmp)
                .toList();
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