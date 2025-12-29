package com.kalabay.cloudstorage.share;

import com.kalabay.cloudstorage.file.FileRepository;
import com.kalabay.cloudstorage.file.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FileShareService {

    private final FileShareRepository shares;
    private final FileRepository files;
    private final Path storageRoot;

    public FileShareService(FileShareRepository shares, FileRepository files, @Value("${storage.root-dir:./data/storage}") String rootDir) {
        this.shares = shares;
        this.files = files;
        this.storageRoot = Paths.get(rootDir)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional
    public FileShare createShare(String username, Long fileId, Instant expiresAt) {
        StoredFile file = files.findByIdAndOwner_Username(fileId, username)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalStateException("expiresAt must be in the future");
        }

        String token = UUID.randomUUID()
                .toString()
                .replace("-", "");

        FileShare share = FileShare.builder()
                .file(file)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        return shares.save(share);
    }

    @Transactional(readOnly = true)
    public List<FileShare> listSharesForFile(String username, Long fileId) {
        files.findByIdAndOwner_Username(fileId, username)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        return shares.findAllByFile_IdAndFile_Owner_UsernameOrderByCreatedAtDesc(fileId, username);
    }

    @Transactional
    public void revoke(String username, Long shareId) {
        FileShare share = shares.findByIdAndFile_Owner_Username(shareId, username)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        if (!share.isRevoked()) {
            share.setRevoked(true);
            shares.save(share);
        }
    }

    @Transactional(readOnly = true)
    public PublicDownload resolvePublicDownload(String token) {
        FileShare share = shares.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        if (share.isRevoked()) {
            throw new IllegalStateException("Share revoked");
        }

        Instant expiresAt = share.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalStateException("Share expired");
        }

        StoredFile file = share.getFile();
        Path path = storageRoot.resolve(file.getStorageName());

        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("File not readable");
            }
            return new PublicDownload(resource, file.getOriginalName(), file.getContentType());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid file URL", e);
        }
    }

    public record PublicDownload(Resource resource, String filename, String contentType) {}
}