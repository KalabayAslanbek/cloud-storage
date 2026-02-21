package com.kalabay.cloudstorage.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Simple health-check controller.
 *
 * Used for liveness/readiness checks in local and containerized environments.
 */
@RestController
public class HealthController {

    /**
     * Returns application health status.
     *
     * @return map with a single key "status" and value "OK"
     */
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "OK");
    }
}