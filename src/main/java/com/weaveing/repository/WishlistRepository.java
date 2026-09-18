package com.weaveing.repository;

import com.weaveing.entity.Pattern;
import com.weaveing.entity.User;
import com.weaveing.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository
        extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);

    Optional<WishlistItem> findByUserAndPattern(
            User user,
            Pattern pattern
    );

    boolean existsByUserAndPattern(
            User user,
            Pattern pattern
    );

    long countByPattern(Pattern pattern);

    @Transactional
    void deleteByUserAndPattern(
            User user,
            Pattern pattern
    );

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteByPattern(Pattern pattern);
}