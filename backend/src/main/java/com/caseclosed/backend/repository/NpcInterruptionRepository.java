package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.NpcInterruption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NpcInterruptionRepository extends JpaRepository<NpcInterruption, UUID> {
    List<NpcInterruption> findByGameCaseId(UUID caseId);
    List<NpcInterruption> findByGameCaseIdAndSuspectId(UUID caseId, UUID suspectId);
}
