package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Province;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, UUID> {
    Page<Province> findAllByActiveTrue(Pageable pageable);
    Page<Province> findAllByDepartmentIdAndActiveTrue(UUID departmentId, Pageable pageable);
}
