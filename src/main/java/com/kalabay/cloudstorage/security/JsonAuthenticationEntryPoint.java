package com.kalabay.cloudstorage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalabay.cloudstorage.common.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    public JsonAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Unauthorized",
                request.getRequestURI(),
                null
        );

        mapper.writeValue(response.getOutputStream(), body);
    }
}
