package com.kalabay.cloudstorage.file;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.common.exception.NotFoundException;
import com.kalabay.cloudstorage.folder.Folder;
import com.kalabay.cloudstorage.folder.FolderRepository;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for file storage operations.
 *
 * Responsibilities:
 * - Validate upload input
 * - Store file content on disk under a generated unique storage name
 * - Persist file metadata in the database
 * - Enforce ownership checks by querying entities using (id + username)
 *
 * Note: Physical files are stored under {@code storage.root-dir}.
 */
@Service
public class FileService {

    private final FileRepository files;
    private final UserRepository users;
    private final FolderRepository folders;
    private final Path rootDir;

    /**
     * Creates service instance and ensures that the storage directory exists.
     *
     * @param files file repository
     * @param users user repository
     * @param folders folder repository
     * @param rootDir root directory for physical file storage (from configuration)
     * @throws IllegalStateException if storage directory cannot be created
     */
    public FileService(FileRepository files, UserRepository users, FolderRepository folders, @Value("${storage.root-dir:./data/storage}") String rootDir) {
        this.files = files;
        this.users = users;
        this.folders = folders;
        this.rootDir = Paths.get(rootDir)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }
    }

    /**
     * Stores an uploaded multipart file on disk and saves its metadata in the database.
     *
     * Validates:
     * - file is not null and not empty
     * - original filename is present
     * - user exists
     * - folder exists and belongs to the user (if folderId is provided)
     *
     * @param multipart uploaded file
     * @param username authenticated user's username
     * @param folderId optional destination folder ID; if null, file is stored in root
     * @return saved file entity
     * @throws BadRequestException if file is empty or filename is invalid
     * @throws NotFoundException if user or folder is not found
     * @throws IllegalStateException if physical file cannot be stored
     */
    @Transactional
    public StoredFile upload(MultipartFile multipart, String username, Long folderId) {
        if (multipart == null || multipart.isEmpty()) {
            throw new BadRequestException("{file.upload.empty}");
        }

        String originalName = multipart.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("{file.upload.filename}");
        }

        User owner = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Folder folder = null;
        if (folderId != null) {
            folder = folders.findByIdAndOwner_Username(folderId, username)
                    .orElseThrow(() -> new NotFoundException("Folder not found"));
        }

        String storageName = UUID.randomUUID().toString().replace("-", "");
        Path target = rootDir.resolve(storageName);

        try {
            Files.copy(multipart.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        StoredFile file = StoredFile.builder()
                .owner(owner)
                .folder(folder)
                .originalName(originalName)
                .storageName(storageName)
                .contentType(multipart.getContentType())
                .sizeBytes(multipart.getSize())
                .build();

        return files.save(file);
    }

    /**
     * Returns list of files for the user within the specified folder (or root if folderId is null).
     *
     * @param username authenticated user's username
     * @param folderId optional folder ID; if null, lists root files
     * @return list of stored files ordered by upload time (descending)
     */
    @Transactional(readOnly = true)
    public List<StoredFile> list(String username, Long folderId) {
        if (folderId == null) {
            return files.findAllByOwner_UsernameAndFolderIsNullOrderByUploadedAtDesc(username);
        }
        return files.findAllByOwner_UsernameAndFolder_IdOrderByUploadedAtDesc(username, folderId);
    }

    /**
     * Loads file content as a {@link Resource} for download.
     *
     * Ensures:
     * - file exists and belongs to the user
     * - file exists on disk and is readable
     *
     * @param id file ID
     * @param username authenticated user's username
     * @return download DTO containing resource, original filename and content type
     * @throws NotFoundException if file metadata is not found
     * @throws IllegalStateException if file is not readable or path is invalid
     */
    @Transactional(readOnly = true)
    public FileDownload getFile(Long id, String username) {
        StoredFile file = files.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new NotFoundException("File not found"));

        Path path = rootDir.resolve(file.getStorageName());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("File not readable");
            }
            return new FileDownload(resource, file.getOriginalName(), file.getContentType());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("File path invalid", e);
        }
    }

    /**
     * Deletes a file owned by the user.
     *
     * Removes physical file from disk (if exists) and then deletes metadata from database.
     *
     * @param id file ID
     * @param username authenticated user's username
     * @throws NotFoundException if file metadata is not found
     */
    @Transactional
    public void delete(Long id, String username) {
        StoredFile file = files.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new NotFoundException("File not found"));

        Path path = rootDir.resolve(file.getStorageName());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {}

        files.delete(file);
    }

    /**
     * Moves a file to another folder (or to root if folderId is null).
     *
     * Validates that both the file and the destination folder (if provided)
     * belong to the authenticated user.
     *
     * @param username authenticated user's username
     * @param fileId file ID
     * @param folderId destination folder ID (nullable)
     * @return updated file entity
     * @throws NotFoundException if file or folder is not found
     */
    @Transactional
    public StoredFile move(String username, Long fileId, Long folderId) {
        StoredFile file = files.findByIdAndOwner_Username(fileId, username)
                .orElseThrow(() -> new NotFoundException("File not found"));
        Folder folder = null;
        if (folderId != null) {
            folder = folders.findByIdAndOwner_Username(folderId, username)
                    .orElseThrow(() -> new NotFoundException("Folder not found"));
        }

        file.setFolder(folder);
        return files.save(file);
    }

    public record FileDownload(Resource resource, String filename, String contentType) {}
}