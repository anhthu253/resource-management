package com.example.springboot.controller;
import com.example.springboot.dto.AuthRequestDto;
import com.example.springboot.dto.UserDto;
import com.example.springboot.security.BookingUserDetailsService;
import com.example.springboot.security.CustomUserDetails;
import com.example.springboot.security.JwtUtil;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final BookingUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authManager, BookingUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }
    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<?> handleNonUnique(Exception ex) {
        return ResponseEntity.badRequest().body("Multiple users found where only one expected");
    }
    @PostMapping("/authenticate")
    public ResponseEntity<UserDto> createToken(@RequestBody AuthRequestDto request) throws Exception {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid credentials", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        String token = jwtUtil.generateToken(customUserDetails);
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true)             // only HTTP
                .sameSite("None")           // or "Lax" if using redirects
                .path("/")
                .maxAge(24 * 60 * 60)         // 1 day
                .build();
        UserDto userDto = new UserDto();
        userDto.setUserId(customUserDetails.getId());
        userDto.setEmail(customUserDetails.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(userDto);
    }

    @GetMapping("current-user")
    ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto dto = new UserDto();
        dto.setUserId(userDetails.getId());
        dto.setEmail(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }
}
