package com.localbite.backend.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // BUYER, SELLER, ADMIN

    @Column
    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Removed bidirectional mapping to avoid circular reference issues
    // @ManyToMany(mappedBy = "roles")
    // @JsonIgnore
    // private Set<User> users;

    public enum RoleName {
        BUYER, SELLER, ADMIN
    }
} 