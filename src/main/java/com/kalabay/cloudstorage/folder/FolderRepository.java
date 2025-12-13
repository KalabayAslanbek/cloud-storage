package com.kalabay.cloudstorage.folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByOwner_UsernameAndParentIsNullOrderByCreatedAtAsc(String username);

    List<Folder> findAllByOwner_UsernameAndParent_IdOrderByCreatedAtAsc(String username, Long parentId);

    List<Folder> findAllByOwner_UsernameOrderByCreatedAtAsc(String username);

    Optional<Folder> findByIdAndOwner_Username(Long id, String username);

    boolean existsByOwner_UsernameAndParentIsNullAndName(String username, String name);

    boolean existsByOwner_UsernameAndParent_IdAndName(String username, Long parentId, String name);

    @Query(value = """
        WITH RECURSIVE subtree AS (
            SELECT f.id
            FROM folders f
            JOIN users u ON u.id = f.owner_id
            WHERE f.id = :rootId AND u.username = :username
            UNION ALL
            SELECT c.id
            FROM folders c
            JOIN subtree s ON c.parent_id = s.id
        )
        SELECT id FROM subtree
        """, nativeQuery = true)
    List<Long> findSubtreeIds(@Param("username") String username, @Param("rootId") Long rootId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        DELETE FROM folders f
        USING users u
        WHERE f.id = :rootId
          AND f.owner_id = u.id
          AND u.username = :username
        """, nativeQuery = true)
    int deleteOwnedRoot(@Param("username") String username, @Param("rootId") Long rootId);
}