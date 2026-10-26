package com.b0ss.crm.auth;

import com.b0ss.crm.auth.dto.LoginRequest;
import com.b0ss.crm.auth.dto.LoginResponse;
import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.user.Role;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = user();
        Instant expiresAt = Instant.now().plusSeconds(1800);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.issue(user)).thenReturn(new JwtService.IssuedToken("token-value", expiresAt));

        LoginResponse response = authService.login(new LoginRequest("admin", "Passw0rd!"));

        assertThat(response.token()).isEqualTo("token-value");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().role()).isEqualTo("ADMIN");
    }

    @Test
    void loginThrowsWhenCredentialsAreWrong() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void loginThrowsWhenUserDisappearsAfterAuthentication() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "Passw0rd!")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void currentUserThrowsWhenMissing() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.currentUser("nobody"))
                .isInstanceOf(NotFoundException.class);
    }

    private User user() {
        User user = new User();
        user.setUsername("admin");
        user.setDisplayName("Admin User");
        user.setPassword("hash");
        user.setRole(Role.ADMIN);
        return user;
    }
}
