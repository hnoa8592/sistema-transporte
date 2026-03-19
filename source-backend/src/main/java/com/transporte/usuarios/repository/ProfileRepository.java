package com.transporte.usuarios.repository;

import com.transporte.usuarios.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Page<Profile> findAllByActiveTrue(Pageable pageable);
    Optional<Profile> findByIdAndActiveTrue(UUID id);
    boolean existsByName(String name);

    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.resources WHERE p.id = :id")
    Optional<Profile> findByIdWithResources(UUID id);
}
