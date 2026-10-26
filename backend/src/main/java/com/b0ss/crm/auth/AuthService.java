package com.b0ss.crm.auth;

import com.b0ss.crm.auth.dto.LoginRequest;
import com.b0ss.crm.auth.dto.LoginResponse;
import com.b0ss.crm.auth.dto.UserResponse;
import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.username()));

        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return UserResponse.from(user);
    }
}
