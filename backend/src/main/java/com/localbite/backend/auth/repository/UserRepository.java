package com.localbite.backend.auth.repository;

import com.localbite.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, User.AuthProvider provider);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.email = :email")
    void verifyEmail(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.email = :email")
    void updateFailedLoginAttempts(@Param("email") String email, @Param("attempts") Integer attempts);

    @Modifying
    @Query("UPDATE User u SET u.accountLocked = :locked WHERE u.email = :email")
    void updateAccountLocked(@Param("email") String email, @Param("locked") Boolean locked);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.email = :email")
    void updateLastLogin(@Param("email") String email, @Param("lastLogin") LocalDateTime lastLogin);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true AND u.accountLocked = false")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
} 