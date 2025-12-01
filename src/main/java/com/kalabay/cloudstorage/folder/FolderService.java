package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.folder.dto.*;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class FolderService {

    private final FolderRepository folders;
    private final UserRepository users;

    public FolderService(FolderRepository folders, UserRepository users) {
        this.folders = folders;
        this.users = users;
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