package com.kalabay.cloudstorage.file;

import com.kalabay.cloudstorage.common.exception.BadRequestException;
import com.kalabay.cloudstorage.common.exception.NotFoundException;
import com.kalabay.cloudstorage.folder.Folder;
import com.kalabay.cloudstorage.folder.FolderRepository;
import com.kalabay.cloudstorage.user.User;
import com.kalabay.cloudstorage.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private FileRepository fileRepository;
    private UserRepository userRepository;
    private FolderRepository folderRepository;

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        userRepository = mock(UserRepository.class);
        folderRepository = mock(FolderRepository.class);

        fileService = new FileService(fileRepository, userRepository, folderRepository, tempDir.toString());
    }

    @Test
    void upload_success_writesToDisk_andSavesEntity() throws Exception {
        String username = "alice";
        User owner = User.builder().id(1L).username(username).passwordHash("x").build();
        Folder folder = Folder.builder().id(2L).owner(owner).name("Docs").build();

        byte[] content = "hello".getBytes();
        MockMultipartFile multipart = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                content
        );

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        when(folderRepository.findByIdAndOwner_Username(2L, username)).thenReturn(Optional.of(folder));

        when(fileRepository.save(any(StoredFile.class))).thenAnswer(inv -> inv.getArgument(0));

        StoredFile saved = fileService.upload(multipart, username, 2L);

        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.getFolder()).isSameAs(folder);
        assertThat(saved.getOriginalName()).isEqualTo("hello.txt");
        assertThat(saved.getContentType()).isEqualTo("text/plain");
        assertThat(saved.getSizeBytes()).isEqualTo(content.length);

        assertThat(saved.getStorageName()).isNotBlank();
        assertThat(saved.getStorageName()).hasSize(32);

        Path storedPath = tempDir.resolve(saved.getStorageName());
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(content);

        verify(fileRepository).save(any(StoredFile.class));
    }

    @Test
    void upload_emptyFile_throwsBadRequest() {
        MockMultipartFile empty = new MockMultipartFile(
                "file",
                "a.txt",
                "text/plain",
                new byte[0]
        );

        assertThatThrownBy(() -> fileService.upload(empty, "alice", null))
                .isInstanceOf(BadRequestException.class);

        verify(fileRepository, never()).save(any());
    }

    @Test
    void delete_removesPhysicalFile_andDeletesEntity() throws Exception {
        String username = "alice";
        User owner = User.builder().id(1L).username(username).passwordHash("x").build();

        StoredFile file = StoredFile.builder()
                .id(100L)
                .owner(owner)
                .originalName("x.txt")
                .storageName("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .contentType("text/plain")
                .sizeBytes(1)
                .build();

        Path path = tempDir.resolve(file.getStorageName());
        Files.write(path, new byte[]{1});
        assertThat(Files.exists(path)).isTrue();

        when(fileRepository.findByIdAndOwner_Username(100L, username)).thenReturn(Optional.of(file));

        fileService.delete(100L, username);

        assertThat(Files.exists(path)).isFalse();
        verify(fileRepository).delete(file);
    }
}
