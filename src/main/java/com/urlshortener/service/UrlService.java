package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.UrlListItem;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Click;
import com.urlshortener.entity.Url;
import com.urlshortener.entity.User;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UnauthorizedException;
import com.urlshortener.repository.ClickRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public UrlResponse shorten(ShortenRequest request, String userEmail) {
        validateUrl(request.getOriginalUrl());
        User user = findUserByEmail(userEmail);
        String shortCode = generateUniqueShortCode();
        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .user(user)
                .build();
        url = urlRepository.save(url);
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(baseUrl + "/r/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .build();
    }

    public List<UrlListItem> getUrlsForUser(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<Url> urls = urlRepository.findAllByUser(user);
        return urls.stream()
                .map(url -> UrlListItem.builder()
                        .id(url.getId())
                        .shortCode(url.getShortCode())
                        .originalUrl(url.getOriginalUrl())
                        .clickCount(clickRepository.countByUrl(url))
                        .createdAt(url.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUrl(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));
        if (!url.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to delete this URL");
        }
        clickRepository.deleteByUrl(url);
        urlRepository.delete(url);
    }

    public AnalyticsResponse getAnalytics(Long id, String userEmail) {
        User user = findUserByEmail(userEmail);
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));
        if (!url.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to view analytics for this URL");
        }
        List<Click> clicks = clickRepository.findAllByUrlOrderByClickedAtDesc(url);
        List<AnalyticsResponse.ClickItem> clickItems = clicks.stream()
                .map(c -> AnalyticsResponse.ClickItem.builder().clickedAt(c.getClickedAt()).build())
                .collect(Collectors.toList());
        return AnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(clicks.size())
                .clicks(clickItems)
                .build();
    }

    @Transactional
    public String redirect(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        clickRepository.save(Click.builder().url(url).build());
        return url.getOriginalUrl();
    }

    private void validateUrl(String urlString) {
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            throw new IllegalArgumentException("URL must use HTTP or HTTPS protocol");
        }
        try {
            new URI(urlString).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + urlString);
        }
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateShortCode();
        } while (urlRepository.existsByShortCode(shortCode));
        return shortCode;
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
        }
        return sb.toString();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
