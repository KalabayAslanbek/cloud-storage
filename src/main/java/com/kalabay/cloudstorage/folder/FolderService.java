package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.common.exception.ConflictException;
import com.kalabay.cloudstorage.common.exception.NotFoundException;
import com.kalabay.cloudstorage.file.FileRepository;
import com.kalabay.cloudstorage.folder.dto.FolderPathItem;
import com.kalabay.cloudstorage.folder.dto.FolderTreeNode;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Service responsible for folder operations and folder-tree logic.
 *
 * Responsibilities:
 * - Create folders with unique name constraint within the same level
 * - List children folders (root or by parent)
 * - Build a hierarchical folder tree representation
 * - Rename and move folders with validation (including cycle prevention)
 * - Delete folder subtree:
 *     - removes physical files from storage
 *     - removes database records (subtree deletion via repository)
 * - Resolve folder path (breadcrumbs)
 *
 * Folder ownership is enforced using repository queries scoped by username.
 */
@Service
public class FolderService {

    private final FolderRepository folders;
    private final UserRepository users;
    private final FileRepository files;
    private final Path storageRoot;

    /**
     * Creates folder service and initializes the physical storage directory.
     *
     * @param folders folder repository
     * @param users user repository
     * @param files file repository (used to delete physical files for subtree deletion)
     * @param rootDir root directory for file storage (config: {@code storage.root-dir})
     * @throws IllegalStateException if storage directory cannot be created
     */
    public FolderService(
            FolderRepository folders,
            UserRepository users,
            FileRepository files,
            @Value("${storage.root-dir:./data/storage}") String rootDir
    ) {
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

    /**
     * Creates a new folder for the given user.
     *
     * Validates name, resolves optional parent folder, and ensures uniqueness of the folder name
     * within the same parent level (root or specific parent folder).
     *
     * @param username owner username
     * @param name folder name (must be non-blank)
     * @param parentId optional parent folder ID (null for root)
     * @return persisted folder entity
     * @throws BadRequestException if name is null/blank
     * @throws NotFoundException if user does not exist or parent folder is not found
     * @throws ConflictException if a folder with the same name already exists on that level
     */
    @Transactional
    public Folder create(String username, String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Folder name cannot be empty");
        }

        User owner = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Folder parent = null;
        if (parentId != null) {
            parent = folders.findByIdAndOwner_Username(parentId, username)
                    .orElseThrow(() -> new NotFoundException("Parent folder not found"));
        }

        String trimmed = name.trim();
        ensureUniqueName(username, parent, trimmed);

        Folder folder = Folder.builder()
                .owner(owner)
                .parent(parent)
                .name(trimmed)
                .build();

        return folders.save(folder);
    }

    /**
     * Returns direct child folders for a user.
     *
     * If {@code parentId} is null, returns root folders.
     * If {@code parentId} is provided, validates that the parent exists and belongs to the user.
     *
     * @param username owner username
     * @param parentId optional parent folder ID
     * @return list of child folder entities ordered by creation time ascending
     * @throws NotFoundException if parent folder does not exist or does not belong to the user
     */
    @Transactional(readOnly = true)
    public List<Folder> listChildren(String username, Long parentId) {
        if (parentId == null) {
            return folders.findAllByOwner_UsernameAndParentIsNullOrderByCreatedAtAsc(username);
        }

        folders.findByIdAndOwner_Username(parentId, username)
                .orElseThrow(() -> new NotFoundException("Parent folder not found"));

        return folders.findAllByOwner_UsernameAndParent_IdOrderByCreatedAtAsc(username, parentId);
    }

    /**
     * Builds a tree representation of all folders for the user.
     *
     * The result contains root nodes with nested children lists.
     *
     * @param username owner username
     * @return list of root tree nodes
     */
    @Transactional(readOnly = true)
    public List<FolderTreeNode> getTree(String username) {
        var all = folders.findAllByOwner_UsernameOrderByCreatedAtAsc(username);

        Map<Long, FolderTreeNode> nodes = new LinkedHashMap<>();
        List<FolderTreeNode> roots = new ArrayList<>();

        all.forEach(folder -> {
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
            FolderTreeNode node = new FolderTreeNode(
                    folder.getId(),
                    folder.getName(),
                    parentId,
                    folder.getCreatedAt(),
                    new ArrayList<>()
            );
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

    /**
     * Renames a folder owned by the user.
     *
     * Ensures the new name is not blank and is unique within the same parent level.
     * If the requested name is the same as current, no changes are made.
     *
     * @param username owner username
     * @param folderId folder ID
     * @param newName new folder name (must be non-blank)
     * @return updated folder entity
     * @throws BadRequestException if newName is null/blank
     * @throws NotFoundException if folder is not found for the given user
     * @throws ConflictException if a folder with the same name already exists on that level
     */
    @Transactional
    public Folder rename(String username, Long folderId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BadRequestException("Folder name cannot be empty");
        }

        Folder folder = folders.findByIdAndOwner_Username(folderId, username)
                .orElseThrow(() -> new NotFoundException("Folder not found"));

        String trimmed = newName.trim();
        if (trimmed.equals(folder.getName())) {
            return folder;
        }

        ensureUniqueName(username, folder.getParent(), trimmed);
        folder.setName(trimmed);

        return folders.save(folder);
    }

    /**
     * Moves a folder to a new parent (or to root if {@code newParentId} is null).
     *
     * Validates folder ownership and destination ownership.
     * Prevents creating cycles by disallowing moving a folder into itself or any of its descendants.
     * Also enforces uniqueness of the folder name on the destination level.
     *
     * @param username owner username
     * @param folderId folder ID to move
     * @param newParentId destination parent folder ID (nullable for root)
     * @return updated folder entity
     * @throws NotFoundException if folder or destination parent folder is not found for the user
     * @throws BadRequestException if attempting to move into itself or its descendant
     * @throws ConflictException if a folder with the same name already exists on destination level
     */
    @Transactional
    public Folder move(String username, Long folderId, Long newParentId) {
        Folder folder = folders.findByIdAndOwner_Username(folderId, username)
                .orElseThrow(() -> new NotFoundException("Folder not found"));

        Folder newParent = null;
        if (newParentId != null) {
            newParent = folders.findByIdAndOwner_Username(newParentId, username)
                    .orElseThrow(() -> new NotFoundException("Target parent folder not found"));

            Folder cursor = newParent;
            while (cursor != null) {
                if (cursor.getId().equals(folder.getId())) {
                    throw new BadRequestException("Cannot move folder into itself or its descendant");
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

    /**
     * Deletes a folder subtree owned by the user.
     *
     * Steps:
     * 1) Resolves subtree folder IDs via repository query
     * 2) Deletes physical files that belong to folders in the subtree
     * 3) Deletes the root folder record (database cascade should remove the subtree)
     *
     * @param username owner username
     * @param folderId root folder ID to delete
     * @throws NotFoundException if folder is not found for the given user
     */
    @Transactional
    public void delete(String username, Long folderId) {
        List<Long> ids = folders.findSubtreeIds(username, folderId);
        if (ids.isEmpty()) {
            throw new NotFoundException("Folder not found");
        }

        List<String> storageNames = files.findStorageNamesInFolders(username, ids);
        for (String storageName : storageNames) {
            try {
                Files.deleteIfExists(storageRoot.resolve(storageName));
            } catch (IOException ignored) {}
        }

        int deleted = folders.deleteOwnedRoot(username, folderId);
        if (deleted == 0) {
            throw new NotFoundException("Folder not found");
        }
    }

    /**
     * Resolves a breadcrumb-style path from root to the specified folder.
     *
     * @param username owner username
     * @param folderId folder ID
     * @return ordered list of path items (root -> ... -> folder)
     * @throws NotFoundException if folder is not found for the given user
     */
    @Transactional(readOnly = true)
    public List<FolderPathItem> getPath(String username, Long folderId) {
        Folder current = folders.findByIdAndOwner_Username(folderId, username)
                .orElseThrow(() -> new NotFoundException("Folder not found"));

        LinkedList<FolderPathItem> path = new LinkedList<>();
        while (current != null) {
            path.addFirst(new FolderPathItem(current.getId(), current.getName()));
            current = current.getParent();
        }
        return path;
    }

    private void ensureUniqueName(String username, Folder parent, String name) {
        boolean exists = (parent == null)
                ? folders.existsByOwner_UsernameAndParentIsNullAndName(username, name)
                : folders.existsByOwner_UsernameAndParent_IdAndName(username, parent.getId(), name);

        if (exists) {
            throw new ConflictException("Folder with the same name already exists at this level");
        }
    }
}
