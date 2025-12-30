package com.kalabay.cloudstorage.file;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.file.dto.FileResponse;
import com.kalabay.cloudstorage.file.dto.MoveFileRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
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
    public FileResponse upload(
            @RequestPart("file") MultipartFile file,
            Authentication auth,
            @RequestParam(value = "folderId", required = false) Long folderId
    ) {
        StoredFile saved = service.upload(file, auth.getName(), folderId);
        return FileResponse.fromEntity(saved);
    }

    @GetMapping
    public List<FileResponse> list(
            Authentication auth,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        var parsed = parseSort(sort);

        var list = service.list(auth.getName(), folderId)
                .stream()
                .map(FileResponse::fromEntity)
                .toList();

        return sortFiles(list, parsed);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        FileService.FileDownload file = service.getFile(id, auth.getName());

        String encodedFilename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8);
        MediaType mediaType = file.contentType() != null
                ? MediaType.parseMediaType(file.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(file.resource());
    }

    @PatchMapping("/{id}/move")
    public FileResponse move(@PathVariable Long id, @RequestBody MoveFileRequest request, Authentication auth) {
        var moved = service.move(auth.getName(), id, request.folderId());
        return FileResponse.fromEntity(moved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth.getName());
    }

    private List<FileResponse> sortFiles(List<FileResponse> items, ParsedSort sort) {
        Comparator<FileResponse> cmp = switch (sort.field) {
            case "createdAt" -> Comparator.comparing(FileResponse::uploadedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "name" -> Comparator.comparing(FileResponse::filename, String.CASE_INSENSITIVE_ORDER);
            case "size" -> Comparator.comparingLong(FileResponse::sizeBytes);
            default -> throw new BadRequestException("Unsupported sort field: " + sort.field);
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
            default -> throw new BadRequestException("Sort direction must be asc|desc");
        };

        return new ParsedSort(field, asc);
    }

    private record ParsedSort(String field, boolean asc) {}
}
