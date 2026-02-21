package com.kalabay.cloudstorage.share;

import com.kalabay.cloudstorage.share.dto.CreateShareRequest;
import com.kalabay.cloudstorage.share.dto.ShareResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing public share links for a specific file.
 *
 * Allows the authenticated owner of a file to:
 * - create a share link (optionally with expiration time),
 * - list existing share links for the file,
 * - revoke an existing share link.
 *
 * Base path: {@code /api/files/{fileId}/share}
 */
@RestController
@RequestMapping("/api/files/{fileId}/share")
public class FileShareController {

    private final FileShareService service;

    public FileShareController(FileShareService service) {
        this.service = service;
    }

    /**
     * Creates a new share link for the given file owned by the authenticated user.
     *
     * @param fileId ID of the file to share
     * @param request request payload containing optional expiration timestamp
     * @param auth current authenticated user (username is used as owner identifier)
     * @return created share link response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse create(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareRequest request,
            Authentication auth
    ) {
        return ShareResponse.fromEntity(
                service.createShare(auth.getName(), fileId, request.expiresAt())
        );
    }

    /**
     * Lists all share links created for a given file by the authenticated user.
     *
     * @param fileId ID of the file
     * @param auth current authenticated user
     * @return list of share link responses
     */
    @GetMapping
    public List<ShareResponse> list(@PathVariable Long fileId, Authentication auth) {
        return service.listSharesForFile(auth.getName(), fileId)
                .stream()
                .map(ShareResponse::fromEntity)
                .toList();
    }

    /**
     * Revokes (deletes) a share link.
     *
     * Note: {@code fileId} is part of the URL for readability, while revocation is performed by {@code shareId}.
     *
     * @param fileId ID of the file (present in route)
     * @param shareId ID of the share link to revoke
     * @param auth current authenticated user
     */
    @DeleteMapping("/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @PathVariable Long fileId,
            @PathVariable Long shareId,
            Authentication auth
    ) {
        service.revoke(auth.getName(), shareId);
    }
}
