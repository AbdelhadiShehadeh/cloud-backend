package com.urlshortener.repository;

import com.urlshortener.entity.Click;
import com.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickRepository extends JpaRepository<Click, Long> {
    List<Click> findAllByUrlOrderByClickedAtDesc(Url url);
    long countByUrl(Url url);
    void deleteByUrl(Url url);
}
