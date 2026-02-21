package com.kalabay.cloudstorage.share;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for public file downloads via share token.
 *
 * Provides anonymous (non-authenticated) access to files
 * using a previously generated share token.
 *
 * Base path: {@code /api/public/files}
 */
@RestController
@RequestMapping("/api/public/files")
public class PublicShareController {

    private final FileShareService service;

    public PublicShareController(FileShareService service) {
        this.service = service;
    }

    /**
     * Downloads a file using a public share token.
     *
     * Validates the share token via {@link FileShareService}:
     * - Share must exist
     * - Share must not be revoked
     * - Share must not be expired
     *
     * Returns file content as a {@link Resource} with proper
     * Content-Type and Content-Disposition headers for browser download.
     *
     * @param token public share token
     * @return HTTP response containing file resource
     */
    @GetMapping("/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        var download = service.resolvePublicDownload(token);

        String encoded = URLEncoder.encode(download.filename(), StandardCharsets.UTF_8);
        MediaType mediaType = download.contentType() != null
                ? MediaType.parseMediaType(download.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(download.resource());
    }
}
