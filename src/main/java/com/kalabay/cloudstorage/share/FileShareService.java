package com.kalabay.cloudstorage.share;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.common.exception.NotFoundException;
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

/**
 * Service responsible for managing file share links.
 *
 * Responsibilities:
 * - Create share links for files owned by a user
 * - List share links for a specific file
 * - Revoke share links
 * - Resolve public download requests by token
 *
 * Share links are token-based and may have:
 * - Optional expiration time
 * - Revocation flag
 *
 * Physical file content is resolved from the configured storage root directory.
 */
@Service
public class FileShareService {

    private final FileShareRepository shares;
    private final FileRepository files;
    private final Path storageRoot;

    /**
     * Creates a new file share service.
     *
     * @param shares share repository
     * @param files file repository
     * @param rootDir root directory for file storage (configured via {@code storage.root-dir})
     */
    public FileShareService(
            FileShareRepository shares,
            FileRepository files,
            @Value("${storage.root-dir:./data/storage}") String rootDir
    ) {
        this.shares = shares;
        this.files = files;
        this.storageRoot = Paths.get(rootDir).toAbsolutePath().normalize();
    }

    /**
     * Creates a new public share link for a file owned by the user.
     *
     * Validates:
     * - File exists and belongs to the user
     * - Expiration time (if provided) is in the future
     *
     * Generates a unique random token used for public access.
     *
     * @param username authenticated user's username
     * @param fileId ID of the file to share
     * @param expiresAt optional expiration timestamp (must be in the future)
     * @return created {@link FileShare} entity
     *
     * @throws NotFoundException if file does not exist or does not belong to user
     * @throws BadRequestException if expiration time is invalid
     */
    @Transactional
    public FileShare createShare(String username, Long fileId, Instant expiresAt) {
        StoredFile file = files.findByIdAndOwner_Username(fileId, username)
                .orElseThrow(() -> new NotFoundException("File not found"));

        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new BadRequestException("expiresAt must be in the future");
        }

        FileShare share = FileShare.builder()
                .file(file)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        return shares.save(share);
    }

    /**
     * Returns all share links created for a specific file.
     *
     * Ensures that the file exists and belongs to the authenticated user.
     *
     * @param username authenticated user's username
     * @param fileId file ID
     * @return list of share links ordered by creation time descending
     *
     * @throws NotFoundException if file does not exist or does not belong to user
     */
    @Transactional(readOnly = true)
    public List<FileShare> listSharesForFile(String username, Long fileId) {
        files.findByIdAndOwner_Username(fileId, username)
                .orElseThrow(() -> new NotFoundException("File not found"));

        return shares.findAllByFile_IdAndFile_Owner_UsernameOrderByCreatedAtDesc(fileId, username);
    }

    /**
     * Revokes a share link owned by the user.
     *
     * Sets the {@code revoked} flag to true if not already revoked.
     *
     * @param username authenticated user's username
     * @param shareId share link ID
     *
     * @throws NotFoundException if share link does not exist or does not belong to user
     */
    @Transactional
    public void revoke(String username, Long shareId) {
        FileShare share = shares.findByIdAndFile_Owner_Username(shareId, username)
                .orElseThrow(() -> new NotFoundException("Share not found"));

        if (!share.isRevoked()) {
            share.setRevoked(true);
            shares.save(share);
        }
    }
    
    /**
     * Resolves a public download request by share token.
     *
     * Validates:
     * - Share exists
     * - Share is not revoked
     * - Share is not expired
     * - Physical file exists and is readable
     *
     * Returns a wrapper containing file resource, original filename and content type.
     *
     * @param token public share token
     * @return {@link PublicDownload} record containing file download data
     *
     * @throws NotFoundException if share is not found
     * @throws BadRequestException if share is revoked or expired
     * @throws IllegalStateException if file cannot be read from storage
     */
    @Transactional(readOnly = true)
    public PublicDownload resolvePublicDownload(String token) {
        FileShare share = shares.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Share not found"));

        if (share.isRevoked()) {
            throw new BadRequestException("Share revoked");
        }

        Instant expiresAt = share.getExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new BadRequestException("Share expired");
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

    /**
     * Represents resolved public download data.
     *
     * @param resource file resource to stream
     * @param filename original filename for download
     * @param contentType file MIME type
     */
    public record PublicDownload(Resource resource, String filename, String contentType) {}
}
