package com.project.howmuch_be.controller;

import com.project.howmuch_be.dto.UserDTO;
import com.project.howmuch_be.dto.VerifyResponse;
import com.project.howmuch_be.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "사용자 확인", description = "카카오 로그인 후 사용자 존재 여부 확인")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(
            @Parameter(description = "카카오 UID", required = true)
            @RequestBody String uid) {
        uid = uid.replaceAll("^\"|\"$", "");
        UserDTO userDTO = authService.verifyUser(uid);
        if (userDTO == null) {
            return ResponseEntity.ok(new VerifyResponse(false, null));
        }
        return ResponseEntity.ok(new VerifyResponse(true, userDTO));
    }

    @Operation(summary = "회원가입", description = "신규 사용자 등록")
    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(
            @Parameter(description = "사용자 정보", required = true)
            @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(authService.signup(userDTO));
    }
}