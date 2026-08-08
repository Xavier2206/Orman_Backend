package com.orman.backend.auth.repository;

import com.orman.backend.auth.entity.OtpChallenge;
import com.orman.backend.auth.model.ClientType;
import com.orman.backend.auth.model.OtpPurpose;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findByLoginAndClientTypeAndPurposeAndStatus(String login, ClientType clientType,
            OtpPurpose purpose, com.orman.backend.auth.model.OtpChallengeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.id = :id")
    Optional<OtpChallenge> findByIdForUpdate(@Param("id") UUID id);
}
