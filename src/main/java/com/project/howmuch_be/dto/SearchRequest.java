package com.project.howmuch_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequest {
    @Schema(description = "검색 키워드", example = "마라탕", required = true)
    private String keyword;
} 