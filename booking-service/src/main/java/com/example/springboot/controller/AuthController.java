package com.example.springboot.controller;
import com.example.springboot.dto.AuthRequestDto;
import com.example.springboot.dto.UserDto;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.security.BookingUserDetailsService;
import com.example.springboot.security.CustomUserDetails;
import com.example.springboot.security.JwtUtil;
import com.example.springboot.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authManager, BookingUserDetailsService userDetailsService, UserService userService, UserMapper userMapper, JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }
    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<?> handleNonUnique(Exception ex) {
        return ResponseEntity.badRequest().body("Multiple users found where only one expected");
    }

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody UserDto userDto){
        try{
            userService.createUser(userMapper.mapUserDtoToUser(userDto));
            return ResponseEntity.ok().body("");
        }
        catch(IllegalArgumentException ex){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
        catch(DataIntegrityViolationException ex){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }
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
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)      // 🔥 deletes the cookie
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
    @GetMapping("current-user")
    ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto dto = new UserDto();
        dto.setUserId(userDetails.getId());
        dto.setEmail(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }
}
