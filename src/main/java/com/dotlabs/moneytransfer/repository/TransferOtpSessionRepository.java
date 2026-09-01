package com.dotlabs.moneytransfer.repository;

import com.dotlabs.moneytransfer.entity.TransferOtpSession;
import com.dotlabs.moneytransfer.enums.OtpSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferOtpSessionRepository extends JpaRepository<TransferOtpSession, Long> {
    Optional<TransferOtpSession> findBySessionId(String sessionId);
    Optional<TransferOtpSession> findBySessionIdAndUsername(String sessionId, String username);
}
