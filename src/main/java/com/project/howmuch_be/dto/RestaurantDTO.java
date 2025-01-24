package com.project.howmuch_be.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RestaurantDTO {
    private Long id;
    private String name;
    private List<MenuDTO> menus;
    private LocationDTO location;
    private int reviewCount;
    private List<String> reviews;

    @Getter @Setter
    @NoArgsConstructor
    public static class MenuDTO {
        private String name;
        private int price;
    }

    @Getter @Setter
    @NoArgsConstructor
    public static class LocationDTO {
        private double x;
        private double y;
    }
}
