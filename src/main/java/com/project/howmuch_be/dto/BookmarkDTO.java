package com.project.howmuch_be.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BookmarkDTO {
    private Long id;
    private String uid;
    private RestaurantDTO restaurant;
    private LocalDateTime createdAt;
}
