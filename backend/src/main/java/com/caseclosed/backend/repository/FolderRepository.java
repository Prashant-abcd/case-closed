package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
    List<Folder> findByGameCaseId(UUID caseId);
    Optional<Folder> findByGameCaseIdAndSuspectId(UUID caseId, UUID suspectId);
}
