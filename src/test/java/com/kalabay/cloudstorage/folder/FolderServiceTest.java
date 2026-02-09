package com.kalabay.cloudstorage.folder;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.common.exception.ConflictException;
import com.kalabay.cloudstorage.common.exception.NotFoundException;
import com.kalabay.cloudstorage.file.FileRepository;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    private FolderRepository folderRepository;
    private UserRepository userRepository;
    private FileRepository fileRepository;

    @TempDir
    Path tempDir;

    private FolderService folderService;

    @BeforeEach
    void setUp() {
        folderRepository = mock(FolderRepository.class);
        userRepository = mock(UserRepository.class);
        fileRepository = mock(FileRepository.class);

        folderService = new FolderService(folderRepository, userRepository, fileRepository, tempDir.toString());
    }

    @Test
    void create_rootFolder_success() {
        String username = "alice";
        User owner = User.builder().id(10L).username(username).passwordHash("x").build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        when(folderRepository.existsByOwner_UsernameAndParentIsNullAndName(username, "Docs")).thenReturn(false);

        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        Folder created = folderService.create(username, " Docs ", null);

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getParent()).isNull();
        assertThat(created.getName()).isEqualTo("Docs");

        verify(folderRepository).save(any(Folder.class));
        verify(folderRepository, never()).existsByOwner_UsernameAndParent_IdAndName(anyString(), anyLong(), anyString());
    }

    @Test
    void create_insideParent_success() {
        String username = "alice";
        User owner = User.builder().id(10L).username(username).passwordHash("x").build();

        Folder parent = Folder.builder().id(1L).owner(owner).name("Parent").parent(null).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        when(folderRepository.findByIdAndOwner_Username(1L, username)).thenReturn(Optional.of(parent));
        when(folderRepository.existsByOwner_UsernameAndParent_IdAndName(username, 1L, "Child")).thenReturn(false);
        when(folderRepository.save(any(Folder.class))).thenAnswer(inv -> inv.getArgument(0));

        Folder created = folderService.create(username, "Child", 1L);

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getParent()).isSameAs(parent);
        assertThat(created.getName()).isEqualTo("Child");

        verify(folderRepository).save(any(Folder.class));
        verify(folderRepository).existsByOwner_UsernameAndParent_IdAndName(username, 1L, "Child");
    }

    @Test
    void create_duplicateName_throwsConflict() {
        String username = "alice";
        User owner = User.builder().id(10L).username(username).passwordHash("x").build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        when(folderRepository.existsByOwner_UsernameAndParentIsNullAndName(username, "Docs")).thenReturn(true);

        assertThatThrownBy(() -> folderService.create(username, "Docs", null))
                .isInstanceOf(ConflictException.class);

        verify(folderRepository, never()).save(any());
    }

    @Test
    void move_intoItself_throwsBadRequest() {
        String username = "alice";
        User owner = User.builder().id(10L).username(username).passwordHash("x").build();

        Folder folder = Folder.builder().id(5L).owner(owner).name("A").parent(null).build();

        when(folderRepository.findByIdAndOwner_Username(5L, username))
                .thenReturn(Optional.of(folder));

        assertThatThrownBy(() -> folderService.move(username, 5L, 5L))
                .isInstanceOf(BadRequestException.class);

        verify(folderRepository, never()).save(any());
    }

}
