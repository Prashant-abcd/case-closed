package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.Suspect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuspectRepository extends JpaRepository<Suspect, UUID> {
    List<Suspect> findByGameCaseId(UUID caseId);
}
