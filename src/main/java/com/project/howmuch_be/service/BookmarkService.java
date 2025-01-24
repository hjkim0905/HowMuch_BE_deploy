package com.project.howmuch_be.service;

import com.project.howmuch_be.dto.BookmarkDTO;
import com.project.howmuch_be.dto.RestaurantDTO;
import com.project.howmuch_be.entity.Bookmark;
import com.project.howmuch_be.entity.Menu;
import com.project.howmuch_be.entity.Restaurant;
import com.project.howmuch_be.entity.Review;
import com.project.howmuch_be.entity.User;
import com.project.howmuch_be.repository.BookmarkRepository;
import com.project.howmuch_be.repository.RestaurantRepository;
import com.project.howmuch_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    public List<BookmarkDTO> getUserBookmarks(String uid) {
        return bookmarkRepository.findByUserUidOrderByCreatedAtDesc(uid)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BookmarkDTO addBookmark(BookmarkDTO bookmarkDTO) {
        User user = userRepository.findByUid(bookmarkDTO.getUid())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Restaurant restaurant;
        if (bookmarkDTO.getRestaurant().getId() != null) {
            restaurant = restaurantRepository.findById(bookmarkDTO.getRestaurant().getId())
                    .orElseGet(() -> createRestaurant(bookmarkDTO));
        } else {
            restaurant = createRestaurant(bookmarkDTO);
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setRestaurant(restaurant);

        return convertToDTO(bookmarkRepository.save(bookmark));
    }

    private Restaurant createRestaurant(BookmarkDTO bookmarkDTO) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(bookmarkDTO.getRestaurant().getName());
        
        // Convert MenuDTO to Menu entities
        List<Menu> menus = new ArrayList<>();
        for (RestaurantDTO.MenuDTO menuDTO : bookmarkDTO.getRestaurant().getMenus()) {
            Menu menu = new Menu();
            menu.setName(menuDTO.getName());
            menu.setPrice(menuDTO.getPrice());
            menu.setRestaurant(restaurant);
            menus.add(menu);
        }
        restaurant.setMenus(menus);
        
        // Set location
        restaurant.setLocationX(bookmarkDTO.getRestaurant().getLocation().getX());
        restaurant.setLocationY(bookmarkDTO.getRestaurant().getLocation().getY());
        
        restaurant.setReviewCount(bookmarkDTO.getRestaurant().getReviewCount());
        
        // Convert review strings to Review entities
        List<Review> reviews = new ArrayList<>();
        for (String reviewContent : bookmarkDTO.getRestaurant().getReviews()) {
            Review review = new Review();
            review.setContent(reviewContent);
            review.setRestaurant(restaurant);
            reviews.add(review);
        }
        restaurant.setReviews(reviews);
        
        return restaurantRepository.save(restaurant);
    }

    public void removeBookmark(Long id) {
        try {
            Bookmark bookmark = bookmarkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("북마크를 찾을 수 없습니다. ID: " + id));
            
            Restaurant restaurant = bookmark.getRestaurant();
            
            // 먼저 북마크 삭제
            bookmarkRepository.deleteById(id);
            
            // 해당 레스토랑을 참조하는 다른 북마크가 없는 경우에만 레스토랑과 관련 데이터 삭제
            if (bookmarkRepository.findByRestaurantId(restaurant.getId()).isEmpty()) {
                restaurantRepository.delete(restaurant); // CASCADE 설정으로 인해 메뉴와 리뷰도 함께 삭제됨
            }
            
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("북마크를 찾을 수 없습니다. ID: " + id);
        }
    }

    private BookmarkDTO convertToDTO(Bookmark bookmark) {
        BookmarkDTO dto = new BookmarkDTO();
        dto.setId(bookmark.getId());
        dto.setUid(bookmark.getUser().getUid());
        dto.setRestaurant(convertToRestaurantDTO(bookmark.getRestaurant()));
        dto.setCreatedAt(bookmark.getCreatedAt());
        return dto;
    }
    
    private RestaurantDTO convertToRestaurantDTO(Restaurant restaurant) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setId(restaurant.getId());
        dto.setName(restaurant.getName());
        
        // Convert menus
        List<RestaurantDTO.MenuDTO> menuDTOs = restaurant.getMenus().stream()
            .map(menu -> {
                RestaurantDTO.MenuDTO menuDTO = new RestaurantDTO.MenuDTO();
                menuDTO.setName(menu.getName());
                menuDTO.setPrice(menu.getPrice());
                return menuDTO;
            })
            .collect(Collectors.toList());
        dto.setMenus(menuDTOs);
        
        // Convert location
        RestaurantDTO.LocationDTO locationDTO = new RestaurantDTO.LocationDTO();
        locationDTO.setX(restaurant.getLocationX());
        locationDTO.setY(restaurant.getLocationY());
        dto.setLocation(locationDTO);
        
        dto.setReviewCount(restaurant.getReviewCount());
        
        // Convert reviews
        List<String> reviewContents = restaurant.getReviews().stream()
            .map(Review::getContent)
            .collect(Collectors.toList());
        dto.setReviews(reviewContents);
        
        return dto;
    }
}