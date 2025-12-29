package com.kalabay.cloudstorage.share;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/public/files")
public class PublicShareController {

    private final FileShareService service;

    public PublicShareController(FileShareService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        try {
            var download = service.resolvePublicDownload(token);

            String encoded = URLEncoder.encode(download.filename(), StandardCharsets.UTF_8);
            MediaType mediaType = download.contentType() != null ? MediaType.parseMediaType(download.contentType()) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .body(download.resource());

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share is not available");
        }
    }
}
