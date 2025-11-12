package com.kalabay.cloudstorage.files;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok", "true");
    }
}