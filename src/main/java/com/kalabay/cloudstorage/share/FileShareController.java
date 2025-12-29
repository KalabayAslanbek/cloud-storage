package com.kalabay.cloudstorage.share;

import com.kalabay.cloudstorage.share.dto.CreateShareRequest;
import com.kalabay.cloudstorage.share.dto.ShareResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/files/{fileId}/share")
public class FileShareController {

    private final FileShareService service;

    public FileShareController(FileShareService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse create(@PathVariable Long fileId, @RequestBody CreateShareRequest request, Authentication auth) {
        try {
            var share = service.createShare(auth.getName(), fileId, request.expiresAt());
            return ShareResponse.fromEntity(share);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public List<ShareResponse> list(@PathVariable Long fileId, Authentication auth) {
        try {
            return service.listSharesForFile(auth.getName(), fileId)
                    .stream()
                    .map(ShareResponse::fromEntity)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @PathVariable Long fileId,
            @PathVariable Long shareId,
            Authentication auth
    ) {
        try {
            service.revoke(auth.getName(), shareId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
