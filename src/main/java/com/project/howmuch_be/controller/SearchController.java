package com.project.howmuch_be.controller;

import com.project.howmuch_be.dto.RestaurantDTO;
import com.project.howmuch_be.dto.SearchRequest;
import com.project.howmuch_be.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "음식점 검색 API")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "음식점 검색", description = "키워드로 음식점 검색")
    @PostMapping
    public ResponseEntity<List<RestaurantDTO>> searchRestaurants(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.searchRestaurants(request.getKeyword()));
    }
}
