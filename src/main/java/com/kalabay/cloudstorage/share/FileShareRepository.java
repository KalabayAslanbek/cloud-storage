package com.kalabay.cloudstorage.share;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    Optional<FileShare> findByToken(String token);

    List<FileShare> findAllByFile_IdAndFile_Owner_UsernameOrderByCreatedAtDesc(Long fileId, String username);

    Optional<FileShare> findByIdAndFile_Owner_Username(Long shareId, String username);
}
