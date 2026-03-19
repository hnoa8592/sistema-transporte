package com.transporte.usuarios.repository;

import com.transporte.usuarios.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findAllByActiveTrue(Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile p LEFT JOIN FETCH p.resources WHERE u.username = :username AND u.active = true")
    Optional<User> findByUsernameWithProfileAndResources(String username);
}
