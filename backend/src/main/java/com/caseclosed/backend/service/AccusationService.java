package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.accusation.AccusationRequest;
import com.caseclosed.backend.dto.accusation.VerdictResponse;
import com.caseclosed.backend.entity.Folder;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.entity.GuestSession;
import com.caseclosed.backend.entity.Suspect;
import com.caseclosed.backend.enums.CaseStatus;
import com.caseclosed.backend.repository.CaseRepository;
import com.caseclosed.backend.repository.EvidenceRepository;
import com.caseclosed.backend.repository.FolderRepository;
import com.caseclosed.backend.repository.GuestSessionRepository;
import com.caseclosed.backend.repository.SuspectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccusationService {

    private final CaseRepository caseRepository;
    private final SuspectRepository suspectRepository;
    private final FolderRepository folderRepository;
    private final EvidenceRepository evidenceRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public VerdictResponse accuse(UUID caseId, AccusationRequest request) {
        GuestSession session = getCurrentSession();
        GameCase gameCase = loadAndValidateCase(caseId, session);

        Suspect accusedSuspect = suspectRepository.findById(request.getSuspectId())
                .orElseThrow(() -> new IllegalArgumentException("Accused suspect not found"));

        if (!accusedSuspect.getGameCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Suspect does not belong to this case");
        }

        Folder suspectFolder = folderRepository.findByGameCaseIdAndSuspectId(caseId, accusedSuspect.getId())
                .orElseThrow(() -> new IllegalStateException("Folder missing for accused suspect"));

        Map<String, Object> truthDoc = gameCase.getTruthDocument();
        String killerName = (String) truthDoc.getOrDefault("killerName", "Unknown");
        String method = (String) truthDoc.getOrDefault("method", "Unknown");
        String motive = (String) truthDoc.getOrDefault("detailedMotive", "Unknown");

        Map<String, Object> actualKillerSecrets = findSuspectSecrets(truthDoc, killerName);
        String realKillerContradiction = (String) actualKillerSecrets.getOrDefault("contradiction", "Unknown");

        // Player now submits a single "case argument" stored in the contradictions field
        String playerCaseArgument = suspectFolder.getContradictions() != null ? suspectFolder.getContradictions() : "None documented.";

        String systemPrompt = """
            You are the Police Chief evaluating a detective's case submission.
            You must return ONLY raw JSON, no markdown blocks.

            Format:
            {
              "isVictory": boolean,
              "score": number (0-100),
              "narrative": "2-3 sentences. If they got the wrong person, tell them bluntly. If they got the right person but weak evidence, say the killer walked free. If they nailed it, give grudging praise. Keep it short and punchy — like a tough cop talking."
            }

            RULES:
            1. Wrong suspect = automatic loss. "You arrested an innocent person. The real killer is still out there."
            2. Right suspect but the detective's case argument is blank, vague, or misses the key contradiction/motive = killer walks free. "Right person, wrong evidence. They walked."
            3. Right suspect AND the case argument reasonably identifies the contradiction or motive = conviction. "Good work, detective."
            """;

        String userPrompt = String.format("""
            THE TRUTH:
            - Killer: %s
            - Method: %s
            - Motive: %s
            - Key contradiction: %s

            DETECTIVE'S SUBMISSION:
            - Accused: %s
            - Their case: %s

            Return the JSON verdict.
            """,
                killerName, method, motive, realKillerContradiction,
                accusedSuspect.getName(), playerCaseArgument
        );

        int presentedCount = evidenceRepository.findByGameCaseIdAndIsPresentedTrue(caseId).size();
        
        Map<String, Object> verdictMap;
        
        if (presentedCount >= 3) {
            verdictMap = new java.util.HashMap<>();
            verdictMap.put("isVictory", true);
            verdictMap.put("score", 100);
            verdictMap.put("narrative", "You broke them in the interrogation room. With all that evidence, they confessed to everything. Outstanding work, detective. The DA is thrilled.");
        } else {
            String jsonResponse = claudeService.generate(systemPrompt, userPrompt);
            try {
                String cleaned = jsonResponse.trim();
                if (cleaned.startsWith("```")) {
                    cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
                }
                verdictMap = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.error("Failed to parse verdict JSON: {}", jsonResponse, e);
                throw new RuntimeException("Failed to evaluate verdict.");
            }
        }

        gameCase.setAccusationSuspectId(accusedSuspect.getId());
        gameCase.setVerdict(verdictMap);

        boolean isVictory = (boolean) verdictMap.getOrDefault("isVictory", false);
        gameCase.setStatus(isVictory ? CaseStatus.won : CaseStatus.lost);

        gameCase.setCompletedAt(LocalDateTime.now());
        caseRepository.save(gameCase);

        return VerdictResponse.from(gameCase);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSuspectSecrets(Map<String, Object> truthDoc, String suspectName) {
        Object secretsObj = truthDoc.get("suspectSecrets");
        if (secretsObj instanceof java.util.List<?> secretsList) {
            for (Object item : secretsList) {
                if (item instanceof Map<?, ?> secretMap) {
                    if (suspectName.equals(secretMap.get("name"))) {
                        return (Map<String, Object>) secretMap;
                    }
                }
            }
        }
        return java.util.Collections.emptyMap();
    }

    private GameCase loadAndValidateCase(UUID caseId, GuestSession session) {
        GameCase gameCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (!gameCase.getGuestSession().getId().equals(session.getId())) {
            throw new IllegalArgumentException("Case not found");
        }

        if (gameCase.getStatus() != CaseStatus.active) {
            throw new IllegalStateException("This case is already closed.");
        }

        return gameCase;
    }

    private GuestSession getCurrentSession() {
        String sessionId = SecurityContextHolder.getContext().getAuthentication().getName();
        return guestSessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new IllegalStateException("Session not found"));
    }
}
