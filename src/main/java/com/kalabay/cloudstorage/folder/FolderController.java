package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.folder.dto.CreateFolderRequest;
import com.kalabay.cloudstorage.folder.dto.FolderResponse;
import com.kalabay.cloudstorage.folder.dto.FolderTreeNode;
import com.kalabay.cloudstorage.folder.dto.MoveFolderRequest;
import com.kalabay.cloudstorage.folder.dto.RenameFolderRequest;
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
    public List<FolderResponse> listChildren(@RequestParam(value = "parentId", required = false) Long parentId, Authentication auth) {
        try {
            return service.listChildren(auth.getName(), parentId).stream().map(FolderResponse::fromEntity).toList();
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
}