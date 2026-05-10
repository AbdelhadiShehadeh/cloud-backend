package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShortenRequest {

    @NotBlank(message = "Original URL is required")
    private String originalUrl;
}
