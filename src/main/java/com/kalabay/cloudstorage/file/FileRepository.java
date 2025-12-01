package com.kalabay.cloudstorage.file;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findAllByOwner_UsernameOrderByUploadedAtDesc(String username);

    List<StoredFile> findAllByOwner_UsernameAndFolder_IdOrderByUploadedAtDesc(String username, Long folderId);

    List<StoredFile> findAllByOwner_UsernameAndFolderIsNullOrderByUploadedAtDesc(String username);

    Optional<StoredFile> findByIdAndOwner_Username(Long id, String username);
}