package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByGameCaseId(UUID caseId);
    Optional<Conversation> findByGameCaseIdAndSuspectId(UUID caseId, UUID suspectId);
}
