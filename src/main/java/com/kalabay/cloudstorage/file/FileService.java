package com.kalabay.cloudstorage.file;

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

@Service
public class FileService {

    private final FileRepository files;
    private final UserRepository users;
    private final Path rootDir;

    public FileService(FileRepository files, UserRepository users, @Value("${storage.root-dir:./data/storage}") String rootDir) {
        this.files = files;
        this.users = users;
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }
    }

    @Transactional
    public StoredFile upload(MultipartFile multipart, String username) {
        if (multipart.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        User owner = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String storageName = UUID.randomUUID().toString().replace("-", "");
        Path target = rootDir.resolve(storageName);

        try {
            Files.copy(multipart.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        StoredFile file = StoredFile.builder().owner(owner).originalName(multipart.getOriginalFilename()).storageName(storageName).contentType(multipart.getContentType()).sizeBytes(multipart.getSize()).build();

        return files.save(file);
    }

    @Transactional(readOnly = true)
    public List<StoredFile> list(String username) {
        return files.findAllByOwner_UsernameOrderByUploadedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public FileDownload getFile(Long id, String username) {
        StoredFile file = files.findByIdAndOwner_Username(id, username).orElseThrow(() -> new IllegalArgumentException("File not found"));

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

    @Transactional
    public void delete(Long id, String username) {
        StoredFile file = files.findByIdAndOwner_Username(id, username).orElseThrow(() -> new IllegalArgumentException("File not found"));

        Path path = rootDir.resolve(file.getStorageName());
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {}

        files.delete(file);
    }

    public record FileDownload(Resource resource, String filename, String contentType) {}
}