package com.kalabay.cloudstorage.user.dto;

public record TokenResponse(String token, String tokenType, long expiresIn) {}