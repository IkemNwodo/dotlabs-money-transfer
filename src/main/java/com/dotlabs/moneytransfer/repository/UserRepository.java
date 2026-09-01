package com.dotlabs.moneytransfer.repository;

import com.dotlabs.moneytransfer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByAccountNumber(String accountNumber);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByAccountNumber(String accountNumber);
}
