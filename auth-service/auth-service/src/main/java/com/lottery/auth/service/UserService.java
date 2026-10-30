package com.lottery.auth.service;

import com.lottery.auth.dto.LoginRequest;
import com.lottery.auth.dto.LoginResponse;
import com.lottery.auth.dto.RegisterRequest;
import com.lottery.auth.dto.RegisterResponse;
import com.lottery.auth.entity.User;
import com.lottery.auth.exception.DuplicateUserException;
import com.lottery.auth.exception.InvalidCredentialsException;
import com.lottery.auth.repository.UserRepository;
import com.lottery.auth.repository.UserRoleRepository;   // NEW
import com.lottery.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;                                    // NEW

@Service
public class UserService {

    // The role every new account starts with. Never read this from the request.
    private static final String DEFAULT_ROLE = "CUSTOMER";   // NEW

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRoleRepository userRoleRepository;     // NEW

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserRoleRepository userRoleRepository) {   // NEW
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRoleRepository = userRoleRepository;             // NEW
    }

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        // NEW: give the account its default role (same transaction as the insert)
        userRoleRepository.assign(savedUser.getId(), DEFAULT_ROLE);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getStatus(),
                savedUser.getCreatedAt()
        );
    }

    public LoginResponse loginUser(LoginRequest request) {

        String usernameOrEmail = request.getUsernameOrEmail();

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
                || !"ACTIVE".equals(user.getStatus())) {
            throw new InvalidCredentialsException(
                    "Invalid username/email or password");
        }

        // NEW: read the user's roles and put them in the token
        List<String> roles = userRoleRepository.findRoleNames(user.getId());
        String token = jwtService.generateToken(user.getId(), user.getUsername(), roles);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                token,
                "Bearer");
    }
}