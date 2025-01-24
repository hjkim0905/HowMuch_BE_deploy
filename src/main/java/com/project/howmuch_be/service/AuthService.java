package com.project.howmuch_be.service;

import com.project.howmuch_be.dto.UserDTO;
import com.project.howmuch_be.entity.User;
import com.project.howmuch_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public UserDTO verifyUser(String uid) {
        return userRepository.findByUid(uid)
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setUid(user.getUid());
                    dto.setEmail(user.getEmail());
                    dto.setNickname(user.getNickname());
                    return dto;
                })
                .orElse(null);
    }

    public UserDTO signup(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUid() == null || userDTO.getUid().trim().isEmpty()) {
            throw new IllegalArgumentException("UID cannot be null or empty");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (userDTO.getNickname() == null || userDTO.getNickname().trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }

        User user = new User();
        user.setUid(userDTO.getUid().trim());
        user.setEmail(userDTO.getEmail().trim());
        user.setNickname(userDTO.getNickname().trim());

        User savedUser = userRepository.save(user);

        userDTO.setUid(savedUser.getUid());
        return userDTO;
    }
}