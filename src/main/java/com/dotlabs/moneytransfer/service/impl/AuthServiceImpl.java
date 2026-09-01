package com.dotlabs.moneytransfer.service.impl;

import com.dotlabs.moneytransfer.dto.request.LoginRequest;
import com.dotlabs.moneytransfer.dto.request.RegisterRequest;
import com.dotlabs.moneytransfer.dto.response.AuthResponse;
import com.dotlabs.moneytransfer.entity.Account;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.Currency;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import com.dotlabs.moneytransfer.repository.AccountRepository;
import com.dotlabs.moneytransfer.repository.UserRepository;
import com.dotlabs.moneytransfer.security.JwtTokenProvider;
import com.dotlabs.moneytransfer.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(authentication);

        log.info("User {} successfully authenticated with role {}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole())
                .accountNumber(user.getAccountNumber())
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new InvalidTransferException("Username is already taken: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new InvalidTransferException("Email is already registered: " + email);
        }

        // Generate unique 10-digit bank account number
        String accountNumber;
        do {
            accountNumber = "100" + String.format("%07d", secureRandom.nextInt(10000000));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        // Create new Account with initial opening balance
        Account newAccount = Account.builder()
                .accountNumber(accountNumber)
                .accountHolderName(request.getFullName().trim())
                .balance(new BigDecimal("100000.00")) // 100k opening bonus
                .currency(Currency.NGN)
                .build();
        accountRepository.save(newAccount);

        // Create new User
        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .accountNumber(accountNumber)
                .build();
        User savedUser = userRepository.save(newUser);

        String token = jwtTokenProvider.generateTokenForUser(savedUser);
        log.info("User {} registered successfully with account {}", username, accountNumber);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .username(savedUser.getUsername())
                .role(savedUser.getRole())
                .accountNumber(savedUser.getAccountNumber())
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .build();
    }

    @Override
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
