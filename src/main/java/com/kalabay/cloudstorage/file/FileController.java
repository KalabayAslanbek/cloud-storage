package com.kalabay.cloudstorage.file;

import com.kalabay.cloudstorage.file.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@RequestPart("file") MultipartFile file, Authentication auth) {
        try {
            StoredFile saved = service.upload(file, auth.getName());
            return FileResponse.fromEntity(saved);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot store file");
        }
    }

    @GetMapping
    public List<FileResponse> list(Authentication auth) {
        return service.list(auth.getName()).stream().map(FileResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        try {
            FileService.FileDownload file = service.getFile(id, auth.getName());

            String encodedFilename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType() != null ? file.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(file.resource());

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read file");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        try {
            service.delete(id, auth.getName());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }
}