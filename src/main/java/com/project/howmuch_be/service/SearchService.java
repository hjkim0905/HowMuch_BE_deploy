package com.project.howmuch_be.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.howmuch_be.dto.RestaurantDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final RestaurantSearchService restaurantSearchService;
    private final ObjectMapper objectMapper;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SearchService.class);

    public List<RestaurantDTO> searchRestaurants(String keyword) {
        String normalizedKeyword = keyword.replaceAll("\\s+", "").toLowerCase();
        List<Map<String, Object>> searchResults = restaurantSearchService.searchRestaurants(keyword);
        return searchResults.stream()
                .map(result -> convertToRestaurantDTO(result, normalizedKeyword))
                .filter(dto -> dto != null && !dto.getMenus().isEmpty())
                .collect(Collectors.toList());
    }

    private RestaurantDTO convertToRestaurantDTO(Map<String, Object> result, String normalizedKeyword) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setName((String) result.get("식당이름"));
        
        // reviewCount 변환 로직 수정
        int reviewCount = 0;
        Object reviewCountObj = result.get("reviewCount");
        if (reviewCountObj != null) {
            if (reviewCountObj instanceof Number) {
                reviewCount = ((Number) reviewCountObj).intValue();
            } else {
                try {
                    reviewCount = objectMapper.convertValue(reviewCountObj, Integer.class);
                } catch (Exception e) {
                    log.error("리뷰 수 변환 실패: {} ({})", reviewCountObj, e.getMessage());
                }
            }
        }
        dto.setReviewCount(reviewCount);

        // 메뉴 처리
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> menus = (List<Map<String, Object>>) result.get("메뉴");
        List<RestaurantDTO.MenuDTO> menuDtos = new ArrayList<>();
        
        if (menus != null) {
            for (Map<String, Object> menu : menus) {
                String menuName = (String) menu.get("메뉴명");
                if (menuName == null) continue;
                
                String normalizedMenuName = menuName.replaceAll("\\s+", "").toLowerCase();
                if (!normalizedMenuName.contains(normalizedKeyword)) continue;
                
                RestaurantDTO.MenuDTO menuDto = new RestaurantDTO.MenuDTO();
                menuDto.setName(menuName);
                
                Object price = menu.get("가격");
                int priceValue = 0;
                if (price instanceof Number) {
                    priceValue = ((Number) price).intValue();
                } else if (price instanceof String) {
                    try {
                        priceValue = Integer.parseInt(((String) price).replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        log.error("메뉴 가격 변환 실패: {}", price);
                    }
                }
                menuDto.setPrice(priceValue);
                menuDtos.add(menuDto);
            }
        }
        dto.setMenus(menuDtos);

        // 위치 처리
        @SuppressWarnings("unchecked")
        Map<String, Object> location = (Map<String, Object>) result.get("위치");
        if (location != null && location.get("x") != null && location.get("y") != null) {
            RestaurantDTO.LocationDTO locationDto = new RestaurantDTO.LocationDTO();
            locationDto.setX(Double.parseDouble(String.valueOf(location.get("x"))));
            locationDto.setY(Double.parseDouble(String.valueOf(location.get("y"))));
            dto.setLocation(locationDto);
        }

        // 리뷰 처리
        @SuppressWarnings("unchecked")
        List<String> reviews = (List<String>) result.get("리뷰");
        if (reviews != null) {
            dto.setReviews(reviews.stream()
                    .filter(review -> review != null && review.length() >= 4)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}