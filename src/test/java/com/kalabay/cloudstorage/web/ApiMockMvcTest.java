package com.kalabay.cloudstorage.web;

import com.kalabay.cloudstorage.file.FileController;
import com.kalabay.cloudstorage.file.FileService;
import com.kalabay.cloudstorage.file.StoredFile;
import com.kalabay.cloudstorage.folder.Folder;
import com.kalabay.cloudstorage.folder.FolderController;
import com.kalabay.cloudstorage.folder.FolderService;
import com.kalabay.cloudstorage.security.JsonAccessDeniedHandler;
import com.kalabay.cloudstorage.security.JsonAuthenticationEntryPoint;
import com.kalabay.cloudstorage.security.SecurityConfig;
import com.kalabay.cloudstorage.security.CustomUserDetailsService;
import com.kalabay.cloudstorage.security.jwt.JwtAuthenticationFilter;
import com.kalabay.cloudstorage.security.jwt.JwtService;
import com.kalabay.cloudstorage.user.UserController;
import com.kalabay.cloudstorage.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        UserController.class,
        FolderController.class,
        FileController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class
})
class ApiMockMvcTest {

    @Autowired
    MockMvc mvc;

    @MockBean UserService userService;
    @MockBean FolderService folderService;
    @MockBean FileService fileService;

    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @Test
    void login_success_returnsTokenJson() throws Exception {
        when(userService.login("alice", "pass")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("test.jwt.token");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("test.jwt.token")))
                .andExpect(content().string(containsString("Bearer")));
    }

    @Test
    void createFolder_unauthorized_returns401() throws Exception {
        mvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Docs","parentId":null}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/folders"));
    }

    @Test
    void createFolder_authorized_returns201_andBody() throws Exception {
        Folder folder = Folder.builder()
                .id(10L)
                .name("Docs")
                .parent(null)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        when(folderService.create(eq("alice"), eq("Docs"), isNull()))
                .thenReturn(folder);

        mvc.perform(post("/api/folders")
                        .with(user("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Docs","parentId":null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Docs"))
                .andExpect(jsonPath("$.parentId").doesNotExist()) // parentId null -> Jackson может не выводить или выводить null
                .andExpect(content().string(containsString("createdAt")));
    }

    @Test
    void uploadFile_unauthorized_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes()
        );

        mvc.perform(multipart("/api/files")
                        .file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/files"));
    }

    @Test
    void uploadFile_authorized_returns201_andBody() throws Exception {
        StoredFile stored = StoredFile.builder()
                .id(55L)
                .originalName("a.txt")
                .sizeBytes(5L)
                .contentType(MediaType.TEXT_PLAIN_VALUE)
                .uploadedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .folder(null)
                .build();

        when(fileService.upload(any(), eq("alice"), isNull()))
                .thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes()
        );

        mvc.perform(multipart("/api/files")
                        .file(file)
                        .with(user("alice")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.filename").value("a.txt"))
                .andExpect(jsonPath("$.sizeBytes").value(5))
                .andExpect(jsonPath("$.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string(containsString("uploadedAt")));
    }
}
