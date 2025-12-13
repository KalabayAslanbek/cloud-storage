package com.kalabay.cloudstorage.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findAllByOwner_UsernameOrderByUploadedAtDesc(String username);

    List<StoredFile> findAllByOwner_UsernameAndFolder_IdOrderByUploadedAtDesc(String username, Long folderId);

    List<StoredFile> findAllByOwner_UsernameAndFolderIsNullOrderByUploadedAtDesc(String username);

    Optional<StoredFile> findByIdAndOwner_Username(Long id, String username);

    List<StoredFile> findAllByOwner_UsernameAndFolder_IdIn(String username, Collection<Long> folderIds);

    @Query(value = """
        SELECT fi.storage_name
        FROM files fi
        JOIN users u ON u.id = fi.owner_id
        WHERE u.username = :username
          AND fi.folder_id IN (:folderIds)
        """, nativeQuery = true)
    List<String> findStorageNamesInFolders(@Param("username") String username, @Param("folderIds") Collection<Long> folderIds);
}