package com.localbite.backend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_history", indexes = {
    @Index(name = "idx_password_history_user_id", columnList = "user_id"),
    @Index(name = "idx_password_history_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /**
     * Check if this password history entry is recent enough to be enforced
     * @param maxDaysOld Maximum age in days for password history enforcement
     * @return true if the password should still be restricted
     */
    public boolean isWithinRestrictionPeriod(int maxDaysOld) {
        return createdAt.isAfter(LocalDateTime.now().minusDays(maxDaysOld));
    }
} 