package com.localbite.backend.auth.repository;

import com.localbite.backend.auth.entity.PasswordHistory;
import com.localbite.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Find password history for a user ordered by creation date (newest first)
     */
    List<PasswordHistory> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find the most recent N password history entries for a user
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user " +
           "ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentPasswordHistory(@Param("user") User user);

    /**
     * Find password history entries newer than a specific date
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user " +
           "AND ph.createdAt > :cutoffDate ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findPasswordHistorySince(@Param("user") User user, 
                                                   @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count password history entries for a user
     */
    long countByUser(User user);

    /**
     * Delete old password history entries beyond the retention limit
     */
    @Query("DELETE FROM PasswordHistory ph WHERE ph.user = :user " +
           "AND ph.id NOT IN (SELECT ph2.id FROM PasswordHistory ph2 " +
           "WHERE ph2.user = :user ORDER BY ph2.createdAt DESC LIMIT :retentionCount)")
    void deleteOldPasswordHistory(@Param("user") User user, @Param("retentionCount") int retentionCount);

    /**
     * Check if a specific password hash exists in user's history
     */
    @Query("SELECT CASE WHEN COUNT(ph) > 0 THEN true ELSE false END " +
           "FROM PasswordHistory ph WHERE ph.user = :user AND ph.passwordHash = :passwordHash")
    boolean existsByUserAndPasswordHash(@Param("user") User user, @Param("passwordHash") String passwordHash);
} 