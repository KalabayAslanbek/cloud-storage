package com.kalabay.cloudstorage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalabay.cloudstorage.common.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    public JsonAccessDeniedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Forbidden",
                request.getRequestURI(),
                null
        );

        mapper.writeValue(response.getOutputStream(), body);
    }
}
