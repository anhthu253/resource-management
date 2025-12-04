package com.example.springboot.controller;
import com.example.springboot.dto.AuthRequest;
import com.example.springboot.security.BookingUserDetailsService;
import com.example.springboot.security.JwtUtil;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins="*")
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
    public ResponseEntity<String> createToken(@RequestBody AuthRequest request) throws Exception {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid credentials", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails.getUsername());
        return new ResponseEntity<>(token, HttpStatus.OK);
    }
}
