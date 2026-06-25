package com.intelligentdoctor.admin.repository;

import com.intelligentdoctor.admin.entity.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, String> {
    Optional<AdminUserEntity> findByHospitalIdAndUsername(String hospitalId, String username);

    boolean existsByHospitalIdAndUsername(String hospitalId, String username);
}
