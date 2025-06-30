package com.localbite.backend.auth.repository;

import com.localbite.backend.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing user roles (BUYER, SELLER, ADMIN)
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Find role by name (e.g., "BUYER", "SELLER", "ADMIN")
    Optional<Role> findByName(String name);
    
    // Check if a role exists
    boolean existsByName(String name);
} 