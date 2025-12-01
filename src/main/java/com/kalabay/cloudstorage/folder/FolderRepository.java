package com.kalabay.cloudstorage.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByOwner_UsernameAndParentIsNullOrderByCreatedAtAsc(String username);

    List<Folder> findAllByOwner_UsernameAndParent_IdOrderByCreatedAtAsc(String username, Long parentId);

    List<Folder> findAllByOwner_UsernameOrderByCreatedAtAsc(String username);

    Optional<Folder> findByIdAndOwner_Username(Long id, String username);

    boolean existsByOwner_UsernameAndParentIsNullAndName(String username, String name);

    boolean existsByOwner_UsernameAndParent_IdAndName(String username, Long parentId, String name);
}