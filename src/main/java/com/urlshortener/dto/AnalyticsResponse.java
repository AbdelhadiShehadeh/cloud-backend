package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private List<ClickItem> clicks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClickItem {
        private LocalDateTime clickedAt;
    }
}
