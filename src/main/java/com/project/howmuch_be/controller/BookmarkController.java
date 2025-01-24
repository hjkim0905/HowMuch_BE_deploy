package com.project.howmuch_be.controller;

import com.project.howmuch_be.dto.BookmarkDTO;
import com.project.howmuch_be.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "즐겨찾기 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "즐겨찾기 목록 조회", description = "사용자의 즐겨찾기 목록 조회")
    @GetMapping("/user/{uid}")
    public ResponseEntity<List<BookmarkDTO>> getUserBookmarks(
            @Parameter(description = "사용자 UID", required = true)
            @PathVariable String uid) {
        return ResponseEntity.ok(bookmarkService.getUserBookmarks(uid));
    }

    @Operation(summary = "즐겨찾기 추가", description = "음식점 즐겨찾기 추가")
    @PostMapping
    public ResponseEntity<BookmarkDTO> addBookmark(
            @Parameter(description = "즐겨찾기 정보", required = true)
            @RequestBody BookmarkDTO bookmarkDTO) {
        return ResponseEntity.ok(bookmarkService.addBookmark(bookmarkDTO));
    }

    @Operation(summary = "즐겨찾기 삭제", description = "즐겨찾기 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeBookmark(@PathVariable Long id) {
        try {
            bookmarkService.removeBookmark(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                               .body(e.getMessage());
        }
    }
}