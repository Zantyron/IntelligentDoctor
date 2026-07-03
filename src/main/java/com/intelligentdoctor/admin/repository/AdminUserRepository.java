package com.intelligentdoctor.admin.repository;

import com.intelligentdoctor.admin.entity.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, String> {
    Optional<AdminUserEntity> findByHospitalIdAndUsername(String hospitalId, String username);

    Optional<AdminUserEntity> findByHospitalIdAndUsernameAndRole(String hospitalId, String username, String role);

    Optional<AdminUserEntity> findByHospitalIdAndId(String hospitalId, String id);

    List<AdminUserEntity> findByHospitalIdOrderByCreatedAtDesc(String hospitalId);

    List<AdminUserEntity> findByHospitalIdAndRoleOrderByCreatedAtDesc(String hospitalId, String role);

    boolean existsByHospitalIdAndUsername(String hospitalId, String username);

    boolean existsByHospitalIdAndUsernameAndRole(String hospitalId, String username, String role);

    long countByHospitalIdAndEnabledTrue(String hospitalId);

    long countByHospitalIdAndRoleAndEnabledTrue(String hospitalId, String role);
}
