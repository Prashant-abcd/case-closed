package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.folder.FolderResponse;
import com.caseclosed.backend.dto.folder.UpdateFolderRequest;
import com.caseclosed.backend.entity.Folder;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.entity.GuestSession;
import com.caseclosed.backend.enums.CaseStatus;
import com.caseclosed.backend.repository.CaseRepository;
import com.caseclosed.backend.repository.FolderRepository;
import com.caseclosed.backend.repository.GuestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final CaseRepository caseRepository;
    private final GuestSessionRepository guestSessionRepository;

    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID caseId, UUID suspectId) {
        GuestSession session = getCurrentSession();
        loadAndValidateCase(caseId, session);

        Folder folder = folderRepository.findByGameCaseIdAndSuspectId(caseId, suspectId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found for this suspect"));

        return FolderResponse.from(folder);
    }

    @Transactional
    public FolderResponse updateFolder(UUID caseId, UUID suspectId, UpdateFolderRequest request) {
        GuestSession session = getCurrentSession();
        loadAndValidateCase(caseId, session);

        Folder folder = folderRepository.findByGameCaseIdAndSuspectId(caseId, suspectId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found for this suspect"));

        if (request.getContradictions() != null) {
            folder.setContradictions(request.getContradictions());
        }
        if (request.getIntentToMurder() != null) {
            folder.setIntentToMurder(request.getIntentToMurder());
        }

        folderRepository.save(folder);

        return FolderResponse.from(folder);
    }

    private GameCase loadAndValidateCase(UUID caseId, GuestSession session) {
        GameCase gameCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!gameCase.getGuestSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Case not found");
        }

        if (gameCase.getStatus() != CaseStatus.active) {
            throw new IllegalStateException("This case is no longer active");
        }

        return gameCase;
    }

    private GuestSession getCurrentSession() {
        String sessionId = SecurityContextHolder.getContext().getAuthentication().getName();
        return guestSessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new IllegalStateException("Session not found"));
    }
}
