package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.file.FileRepository;
import com.kalabay.cloudstorage.folder.dto.FolderTreeNode;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FolderService {

    private final FolderRepository folders;
    private final UserRepository users;
    private final FileRepository files;
    private final Path storageRoot;
    
    public FolderService(FolderRepository folders, UserRepository users, FileRepository files, @Value("${storage.root-dir:./data/storage}") String rootDir) {
        this.folders = folders;
        this.users = users;
        this.files = files;
        this.storageRoot = Paths.get(rootDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory", e);
        }
    }

    @Transactional
    public Folder create(String username, String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Folder name cannot be empty");
        }

        User owner = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Folder parent = null;
        if (parentId != null) {
            parent = folders.findByIdAndOwner_Username(parentId, username).orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        ensureUniqueName(username, parent, name);

        Folder folder = Folder.builder().owner(owner) .parent(parent).name(name.trim()).build();

        return folders.save(folder);
    }

    @Transactional(readOnly = true)
    public List<Folder> listChildren(String username, Long parentId) {
        if (parentId == null) {
            return folders.findAllByOwner_UsernameAndParentIsNullOrderByCreatedAtAsc(username);
        }
        folders.findByIdAndOwner_Username(parentId, username).orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));

        return folders.findAllByOwner_UsernameAndParent_IdOrderByCreatedAtAsc(username, parentId);
    }

    @Transactional(readOnly = true)
    public List<FolderTreeNode> getTree(String username) {
        var all = folders.findAllByOwner_UsernameOrderByCreatedAtAsc(username);

        Map<Long, FolderTreeNode> nodes = new LinkedHashMap<>();
        List<FolderTreeNode> roots = new ArrayList<>();

        all.forEach(folder -> {
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
            FolderTreeNode node = new FolderTreeNode(folder.getId(), folder.getName(), parentId, folder.getCreatedAt(), new ArrayList<>());
            nodes.put(folder.getId(), node);
        });

        all.forEach(folder -> {
            FolderTreeNode node = nodes.get(folder.getId());
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;

            if (parentId == null) {
                roots.add(node);
            } else {
                FolderTreeNode parentNode = nodes.get(parentId);
                if (parentNode != null) {
                    parentNode.children().add(node);
                }
            }
        });

        return roots;
    }

    @Transactional
    public Folder rename(String username, Long folderId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalStateException("Folder name cannot be empty");
        }

        Folder folder = folders.findByIdAndOwner_Username(folderId, username).orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        String trimmed = newName.trim();
        if (trimmed.equals(folder.getName())) {
            return folder;
        }

        ensureUniqueName(username, folder.getParent(), trimmed);
        folder.setName(trimmed);
        return folders.save(folder);
    }

    @Transactional
    public Folder move(String username, Long folderId, Long newParentId) {
        Folder folder = folders.findByIdAndOwner_Username(folderId, username).orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        Folder newParent = null;
        if (newParentId != null) {
            newParent = folders.findByIdAndOwner_Username(newParentId, username).orElseThrow(() -> new IllegalArgumentException("Target parent folder not found"));
            Folder cursor = newParent;
            while (cursor != null) {
                if (cursor.getId().equals(folder.getId())) {
                    throw new IllegalStateException("Cannot move folder into itself or its descendant");
                }
                cursor = cursor.getParent();
            }
        }

        Long currentParentId = folder.getParent() != null ? folder.getParent().getId() : null;
        if (Objects.equals(currentParentId, newParentId)) {
            return folder;
        }

        ensureUniqueName(username, newParent, folder.getName());
        folder.setParent(newParent);
        return folders.save(folder);
    }

    @Transactional
    public void delete(String username, Long folderId) {
        List<Long> ids = folders.findSubtreeIds(username, folderId);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Folder not found");
        }

        List<String> storageNames = files.findStorageNamesInFolders(username, ids);

        for (String storageName : storageNames) {
            try {
                Files.deleteIfExists(storageRoot.resolve(storageName));
            } catch (IOException ignored) {}
        }

        int deleted = folders.deleteOwnedRoot(username, folderId);
        if (deleted == 0) {
            throw new IllegalArgumentException("Folder not found");
        }
    }

    private void ensureUniqueName(String username, Folder parent, String name) {
        boolean exists;
        if (parent == null) {
            exists = folders.existsByOwner_UsernameAndParentIsNullAndName(username, name);
        }
        else {
            exists = folders.existsByOwner_UsernameAndParent_IdAndName(username, parent.getId(), name);
        }
        if (exists) {
            throw new IllegalStateException("Folder with the same name already exists at this level");
        }
    }
}