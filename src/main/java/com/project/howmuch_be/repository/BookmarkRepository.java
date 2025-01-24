package com.project.howmuch_be.repository;

import com.project.howmuch_be.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserUidOrderByCreatedAtDesc(String uid);
    Optional<Bookmark> findByUserUidAndRestaurantId(String uid, Long restaurantId);
    List<Bookmark> findByRestaurantId(Long restaurantId);
}