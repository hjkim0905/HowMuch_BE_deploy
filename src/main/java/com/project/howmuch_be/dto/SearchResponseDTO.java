package com.project.howmuch_be.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SearchResponseDTO {
    private List<RestaurantDTO> restaurants;
}