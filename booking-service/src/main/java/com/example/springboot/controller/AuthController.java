package com.example.springboot.controller;

import com.example.springboot.dto.AuthRequestDto;
import com.example.springboot.dto.UserDto;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.security.CustomUserDetails;
import com.example.springboot.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final UserService userService;
    private final UserMapper userMapper;
    @Value("${max.inactive.interval.minute}")
    private int maxInactiveIntervalMinute;

    public AuthController(AuthenticationManager authManager, UserService userService, UserMapper userMapper) {
        this.authManager = authManager;
        this.userService = userService;
        this.userMapper = userMapper;
    }
    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<?> handleNonUnique(Exception ex) {
        return ResponseEntity.badRequest().body("Multiple users found where only one expected");
    }

    @GetMapping("/ping")
    public ResponseEntity<Void> Ping()
    {
        return ResponseEntity.ok().build();
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
    public ResponseEntity<UserDto> login(
            @RequestBody AuthRequestDto request,
            HttpServletRequest httpRequest
    ) throws Exception {
        Authentication authentication;
        try {
            authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid credentials", e);
        }

        // store in SecurityContext
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        // store context in session so Spring Security sees it on future requests
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        session.setMaxInactiveInterval(maxInactiveIntervalMinute * 60);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserDto userDto = new UserDto();
        userDto.setUserId(userDetails.getId());
        userDto.setEmail(userDetails.getUsername());

        return ResponseEntity.ok(userDto);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Invalidate the session
        HttpSession session = request.getSession(false); // false → don’t create if missing
        if (session != null) {
            session.invalidate();
        }

        // Clear SecurityContext
        SecurityContextHolder.clearContext();

        // Delete JSESSIONID cookie in browser
        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(true)           // match your session cookie config
                .sameSite("None")       // or Lax, depending on your setup
                .path("/")
                .maxAge(0)              // deletes the cookie
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
