package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.cases.CaseResponse;
import com.caseclosed.backend.dto.cases.SuspectResponse;
import com.caseclosed.backend.dto.generation.GeneratedCaseData;
import com.caseclosed.backend.dto.generation.GeneratedSuspectData;
import com.caseclosed.backend.entity.Evidence;
import com.caseclosed.backend.entity.Folder;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.entity.GuestSession;
import com.caseclosed.backend.entity.Suspect;
import com.caseclosed.backend.repository.CaseRepository;
import com.caseclosed.backend.repository.EvidenceRepository;
import com.caseclosed.backend.repository.FolderRepository;
import com.caseclosed.backend.repository.GuestSessionRepository;
import com.caseclosed.backend.repository.SuspectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseGenerationService caseGenerationService;
    private final CaseRepository caseRepository;
    private final SuspectRepository suspectRepository;
    private final FolderRepository folderRepository;
    private final EvidenceRepository evidenceRepository;
    private final GuestSessionRepository guestSessionRepository;

    @Transactional
    public CaseResponse createCase() {
        GuestSession session = getCurrentSession();

        GeneratedCaseData generated = caseGenerationService.generateCase();

        // Enrich truth document with suspect-specific interrogation data.
        Map<String, Object> enrichedTruthDoc = new java.util.HashMap<>(generated.getTruthDocument());
        GeneratedSuspectData s = generated.getSuspect();
        Map<String, Object> secret = new java.util.LinkedHashMap<>();
        secret.put("name", s.getName());
        secret.put("trueAlibi", s.getTrueAlibi());
        secret.put("lieTheyTell", s.getLieTheyTell());
        secret.put("contradiction", s.getContradiction());
        secret.put("motiveToKill", s.getMotiveToKill());
        secret.put("personality", s.getPersonality());
        secret.put("isKiller", s.isKiller());
        
        enrichedTruthDoc.put("suspectSecrets", List.of(secret));

        GameCase gameCase = GameCase.builder()
                .guestSession(session)
                .truthDocument(enrichedTruthDoc)
                .caseBriefing(generated.getCaseBriefing())
                .strikeCount(0)
                .build();

        caseRepository.save(gameCase);

        Suspect suspect = buildSuspect(s, gameCase);
        suspectRepository.save(suspect);

        folderRepository.save(
                Folder.builder()
                        .gameCase(gameCase)
                        .suspect(suspect)
                        .build()
        );

        List<Evidence> evidenceList = new java.util.ArrayList<>();
        
        // Convert "What We Know" facts into initial evidence in the locker
        Map<String, Object> briefing = generated.getCaseBriefing();
        if (briefing != null && briefing.get("whatWeKnow") instanceof List<?> facts) {
            int factIndex = 1;
            for (Object factObj : facts) {
                evidenceList.add(Evidence.builder()
                        .gameCase(gameCase)
                        .name("Initial Police Fact #" + factIndex)
                        .description(factObj.toString())
                        .isFound(true)
                        .isPresented(false)
                        .build());
                factIndex++;
            }
        }

        if (generated.getEvidence() != null && !generated.getEvidence().isEmpty()) {
            evidenceList.addAll(generated.getEvidence().stream()
                    .map(e -> Evidence.builder()
                            .gameCase(gameCase)
                            .name(e.getName())
                            .description(e.getDescription())
                            .isFound(false)
                            .isPresented(false)
                            .build())
                    .toList());
        }

        if (!evidenceList.isEmpty()) {
            evidenceRepository.saveAll(evidenceList);
        }

        List<SuspectResponse> suspectResponses = List.of(SuspectResponse.from(suspect));

        return CaseResponse.from(gameCase, suspectResponses);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID caseId) {
        GuestSession session = getCurrentSession();

        GameCase gameCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!gameCase.getGuestSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Case not found");
        }

        List<SuspectResponse> suspectResponses = suspectRepository.findByGameCaseId(caseId)
                .stream()
                .map(SuspectResponse::from)
                .toList();

        return CaseResponse.from(gameCase, suspectResponses);
    }

    private Suspect buildSuspect(GeneratedSuspectData data, GameCase gameCase) {
        return Suspect.builder()
                .gameCase(gameCase)
                .name(data.getName())
                .age(data.getAge())
                .title(data.getTitle())
                .company(data.getCompany())
                .relationshipToFounder(data.getRelationshipToFounder())
                .avatarSeed(data.getAvatarSeed())
                .isKiller(data.isKiller())
                .isHighProfile(data.isHighProfile())
                .backgroundCheck(data.getBackgroundCheck())
                .build();
    }

    private GuestSession getCurrentSession() {
        String sessionId = SecurityContextHolder.getContext().getAuthentication().getName();
        return guestSessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new IllegalStateException("Session not found"));
    }
}
