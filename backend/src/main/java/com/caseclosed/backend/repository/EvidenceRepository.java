package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByGameCaseId(UUID gameCaseId);
    List<Evidence> findByGameCaseIdAndIsFoundFalse(UUID gameCaseId);
    List<Evidence> findByGameCaseIdAndIsFoundTrue(UUID gameCaseId);
    List<Evidence> findByGameCaseIdAndIsPresentedTrue(UUID gameCaseId);
}
