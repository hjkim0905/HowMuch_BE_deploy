package com.project.howmuch_be.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    private String uid;
    private String email;
    private String nickname;
}
