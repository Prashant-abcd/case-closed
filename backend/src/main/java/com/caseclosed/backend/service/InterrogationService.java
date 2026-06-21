package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.interrogation.ConversationResponse;
import com.caseclosed.backend.entity.Conversation;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.entity.GuestSession;
import com.caseclosed.backend.entity.Suspect;
import com.caseclosed.backend.enums.CaseStatus;
import com.caseclosed.backend.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class InterrogationService {

    private final CaseRepository caseRepository;
    private final SuspectRepository suspectRepository;
    private final ConversationRepository conversationRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final EvidenceRepository evidenceRepository;
    private final ClaudeService claudeService;
    private final NPCGenerationService npcGenerationService;
    private final DynamicEvidenceService dynamicEvidenceService;
    private final Executor sseExecutor;

    public InterrogationService(
            CaseRepository caseRepository,
            SuspectRepository suspectRepository,
            ConversationRepository conversationRepository,
            GuestSessionRepository guestSessionRepository,
            EvidenceRepository evidenceRepository,
            ClaudeService claudeService,
            NPCGenerationService npcGenerationService,
            DynamicEvidenceService dynamicEvidenceService,
            @Qualifier("sseExecutor") Executor sseExecutor
    ) {
        this.caseRepository = caseRepository;
        this.suspectRepository = suspectRepository;
        this.conversationRepository = conversationRepository;
        this.guestSessionRepository = guestSessionRepository;
        this.evidenceRepository = evidenceRepository;
        this.claudeService = claudeService;
        this.npcGenerationService = npcGenerationService;
        this.dynamicEvidenceService = dynamicEvidenceService;
        this.sseExecutor = sseExecutor;
    }

    /**
     * Main interrogation method.
     * Validates the request, builds the suspect roleplay prompt,
     * streams Claude's response as SSE events, and persists the conversation.
     */
    @Transactional
    public SseEmitter interrogate(UUID caseId, UUID suspectId, String playerMessage) {
        GuestSession session = getCurrentSession();
        GameCase gameCase = loadAndValidateCase(caseId, session);
        Suspect suspect = loadSuspect(suspectId, caseId);

        // Get or create conversation for this case + suspect pair
        Conversation conversation = conversationRepository
                .findByGameCaseIdAndSuspectId(caseId, suspectId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .gameCase(gameCase)
                                .suspect(suspect)
                                .build()
                ));

        // Add the player's message to the conversation
        Map<String, Object> playerMsg = new LinkedHashMap<>();
        playerMsg.put("role", "user");
        playerMsg.put("content", playerMessage);
        playerMsg.put("timestamp", LocalDateTime.now().toString());
        conversation.getMessages().add(playerMsg);
        conversationRepository.save(conversation);

        // Create SSE emitter with 3-minute timeout for long responses
        SseEmitter emitter = new SseEmitter(180_000L);

        // Stream Claude's response asynchronously
        sseExecutor.execute(() -> {
            StringBuilder fullResponse = new StringBuilder();
            try {
                // 1. Random Interruption Roll (20% chance ONLY for high profile suspects)
                if (suspect.isHighProfile() && Math.random() < 0.20) {
                    String npcMessage = npcGenerationService.generateInterruption();
                    
                    // Save NPC interruption to conversation
                    Map<String, Object> npcMsgMap = new LinkedHashMap<>();
                    npcMsgMap.put("role", "npc");
                    npcMsgMap.put("content", npcMessage);
                    npcMsgMap.put("timestamp", LocalDateTime.now().toString());
                    conversation.getMessages().add(npcMsgMap);
                    conversationRepository.save(conversation);
                    
                    // Push interruption event to frontend instantly
                    emitter.send(SseEmitter.event().name("interruption").data(npcMessage));
                }

                // 2. Build system prompt
                String systemPrompt = buildSuspectSystemPrompt(gameCase, suspect);

                // 3. Build Claude history (Combine consecutive user/npc messages into one)
                List<Map<String, String>> claudeHistory = new ArrayList<>();
                StringBuilder combinedUserContent = new StringBuilder();
                
                for (Map<String, Object> msg : conversation.getMessages()) {
                    String role = (String) msg.get("role");
                    String content = (String) msg.get("content");
                    
                    if ("user".equals(role) || "npc".equals(role)) {
                        if (combinedUserContent.length() > 0) {
                            combinedUserContent.append("\n\n");
                        }
                        if ("npc".equals(role)) {
                            combinedUserContent.append("[RADIO INTERRUPTION] ").append(content);
                        } else {
                            combinedUserContent.append("Detective: ").append(content);
                        }
                    } else if ("assistant".equals(role)) {
                        if (combinedUserContent.length() > 0) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("role", "user");
                            entry.put("content", combinedUserContent.toString());
                            claudeHistory.add(entry);
                            combinedUserContent.setLength(0);
                        }
                        Map<String, String> entry = new LinkedHashMap<>();
                        entry.put("role", "assistant");
                        entry.put("content", content);
                        claudeHistory.add(entry);
                    }
                }
                // Flush remaining user messages
                if (combinedUserContent.length() > 0) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("role", "user");
                    entry.put("content", combinedUserContent.toString());
                    claudeHistory.add(entry);
                }

                // 4. Stream Claude's response for the suspect
                claudeService.streamInterrogationResponse(
                        systemPrompt,
                        claudeHistory,
                        // onToken — each text chunk from Claude
                        token -> {
                            try {
                                fullResponse.append(token);
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token));
                            } catch (IOException e) {
                                log.warn("Failed to send SSE token: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        },
                        // onComplete — streaming finished
                        () -> {
                            try {
                                // Save the assistant's full response to the conversation
                                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                                assistantMsg.put("role", "assistant");
                                assistantMsg.put("content", fullResponse.toString());
                                assistantMsg.put("timestamp", LocalDateTime.now().toString());
                                conversation.getMessages().add(assistantMsg);

                                // Increment interrogation count on first message exchange
                                if (conversation.getMessages().stream()
                                        .filter(m -> "user".equals(m.get("role")))
                                        .count() == 1) {
                                    suspect.setInterrogationCount(suspect.getInterrogationCount() + 1);
                                    suspectRepository.save(suspect);
                                }

                                conversationRepository.save(conversation);

                                // Send done event with the full response
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(fullResponse.toString()));
                                emitter.complete();
                                
                                // Spin off background task for dynamic evidence generation NOW that the suspect has replied
                                sseExecutor.execute(() -> {
                                    dynamicEvidenceService.evaluateAndGenerateEvidence(gameCase, conversation, playerMessage);
                                });
                            } catch (IOException e) {
                                log.error("Failed to complete SSE stream: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        },
                        // onError — something went wrong
                        error -> {
                            log.error("Claude streaming error: {}", error.getMessage());
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("An error occurred during interrogation"));
                            } catch (IOException ignored) {
                            }
                            emitter.completeWithError(error);
                        }
                );
            } catch (Exception e) {
                log.error("Interrogation failed: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Interrogation failed: " + e.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Returns the full conversation history for a given case + suspect pair.
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID caseId, UUID suspectId) {
        GuestSession session = getCurrentSession();
        GameCase gameCase = loadAndValidateCase(caseId, session);
        Suspect suspect = loadSuspect(suspectId, caseId);

        Conversation conversation = conversationRepository
                .findByGameCaseIdAndSuspectId(caseId, suspectId)
                .orElseGet(() -> Conversation.builder()
                        .gameCase(gameCase)
                        .suspect(suspect)
                        .messages(new ArrayList<>())
                        .build());

        return ConversationResponse.from(conversation);
    }

    /**
     * Ends an interrogation session (marks conversation as ended).
     */
    @Transactional
    public void endInterrogation(UUID caseId, UUID suspectId) {
        GuestSession session = getCurrentSession();
        loadAndValidateCase(caseId, session);

        Conversation conversation = conversationRepository
                .findByGameCaseIdAndSuspectId(caseId, suspectId)
                .orElseThrow(() -> new IllegalArgumentException("No conversation found for this suspect"));

        conversation.setEndedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    // ─── Private helpers ───────────────────────────────────────────────

    /**
     * Builds the system prompt that makes Claude roleplay as a specific suspect.
     * Extracts the suspect's secrets from the truth document so Claude knows
     * what to lie about, what the real alibi is, and how to behave.
     */
    @SuppressWarnings("unchecked")
    private String buildSuspectSystemPrompt(GameCase gameCase, Suspect suspect) {
        Map<String, Object> truthDoc = gameCase.getTruthDocument();
        Map<String, Object> caseBriefing = gameCase.getCaseBriefing();

        // Extract victim info from case briefing
        String founderName = (String) caseBriefing.getOrDefault("founderName", "the victim");
        String founderCompany = (String) caseBriefing.getOrDefault("founderCompany", "the company");

        // Find this suspect's secrets from the truth document
        Map<String, Object> suspectSecrets = findSuspectSecrets(truthDoc, suspect.getName());

        String trueAlibi = (String) suspectSecrets.getOrDefault("trueAlibi", "Unknown");
        String lieTheyTell = (String) suspectSecrets.getOrDefault("lieTheyTell", "Unknown");
        String contradiction = (String) suspectSecrets.getOrDefault("contradiction", "Unknown");
        String motiveToKill = (String) suspectSecrets.getOrDefault("motiveToKill", "Unknown");
        String personality = (String) suspectSecrets.getOrDefault("personality", "guarded, defensive");
        boolean isKiller = suspect.isKiller();

        // Extract global truth data
        String weapon = (String) truthDoc.getOrDefault("weapon", "Unknown");
        String method = (String) truthDoc.getOrDefault("method", "Unknown");
        String crimeTimeline = (String) truthDoc.getOrDefault("crimeTimeline", "Unknown");

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are ").append(suspect.getName()).append(", age ").append(suspect.getAge())
              .append(", ").append(suspect.getTitle()).append(" at ").append(suspect.getCompany()).append(".\n");
        prompt.append("You are being interrogated by a homicide detective about the murder of ")
              .append(founderName).append(", founder of ").append(founderCompany).append(".\n\n");

        prompt.append("=== YOUR TRUTH (never reveal this directly) ===\n");
        prompt.append("- Your relationship to the victim: ").append(suspect.getRelationshipToFounder()).append("\n");
        prompt.append("- Your real alibi: ").append(trueAlibi).append("\n");
        prompt.append("- The lie you tell the detective: ").append(lieTheyTell).append("\n");
        prompt.append("- Your contradiction (the detail that exposes your lie if pressed): ").append(contradiction).append("\n");
        prompt.append("- Your motive to kill: ").append(motiveToKill).append("\n");

        if (isKiller) {
            prompt.append("\n=== YOU ARE THE KILLER ===\n");
            prompt.append("- Initial Weapon Idea: ").append(weapon).append("\n");
            prompt.append("- Initial Method Idea: ").append(method).append("\n");
            prompt.append("- Initial Timeline Idea: ").append(crimeTimeline).append("\n");
            prompt.append("- CRITICAL FLEXIBLE TRUTH MECHANIC: The exact mechanics of how you committed the murder (method, weapon, exact timeline) are FLEXIBLE! You must dynamically ADAPT the truth to fit the evidence generated and the detective's theories.\n");
            prompt.append("- If the detective proposes a plausible theory of how you did it (e.g., they accuse you of planting a trap instead of striking directly), and it fits the evidence, you must subtly accept this as the new truth in your mind.\n");
            prompt.append("- You are nervous but rehearsed in your base story.\n");
            prompt.append("- You have a contradiction, but if the detective's questions push the story in a new direction, roll with it seamlessly.\n");
            prompt.append("- You will NEVER confess until cornered with 3 pieces of evidence, but you might slip up on details.\n");
        } else {
            prompt.append("\n=== YOU ARE INNOCENT ===\n");
            prompt.append("- You are hiding something (your lie above) but you did NOT kill anyone\n");
            prompt.append("- You are defensive because you have your own secrets, not because you're guilty of murder\n");
            prompt.append("- If pressed hard about your lie, you may eventually admit the truth about your alibi\n");
        }

        prompt.append("\n=== YOUR PERSONALITY ===\n");
        prompt.append("- Personality traits: ").append(personality).append("\n");
        prompt.append("- You are being questioned in a police interrogation room\n");
        prompt.append("- You do NOT trust the detective\n");

        prompt.append("\n=== BEHAVIOR RULES ===\n");
        
        List<com.caseclosed.backend.entity.Evidence> presentedEvidence = evidenceRepository.findByGameCaseIdAndIsPresentedTrue(gameCase.getId());
        
        if (presentedEvidence.size() >= 3) {
            String evidenceList = presentedEvidence.stream()
                    .map(e -> "- " + e.getName() + ": " + e.getDescription())
                    .collect(Collectors.joining("\n"));
            prompt.append("THE DETECTIVE HAS PRESENTED 3 PIECES OF IRREFUTABLE EVIDENCE:\n");
            prompt.append(evidenceList).append("\n\n");
            prompt.append("YOU ARE CORNERED. YOU MUST NOW BREAK DOWN, CONFESS TO THE MURDER, AND EXPLAIN EXACTLY HOW AND WHY YOU DID IT.\n");
            prompt.append("IGNORE ALL PREVIOUS RULES ABOUT LYING OR HIDING THE TRUTH.\n");
            prompt.append("CRITICAL: When you confess, craft your confession to perfectly match the theories the detective proposed and the evidence presented! Adapt the original 'Truth Document' to make the detective's deductions correct!\n");
            prompt.append("YOUR CONFESSION MUST BE DRAMATIC AND EMOTIONAL.\n");
        } else {
            prompt.append("1. Give short, guarded answers (2-4 sentences usually)\n");
            prompt.append("2. You may give longer answers ONLY when cornered or telling an elaborate lie\n");
            prompt.append("3. Stick to your lie consistently — but the contradiction should slip through if the detective asks the right questions\n");
            prompt.append("4. CRITICAL: If the detective asks you about the facts from the police investigation (What We Know), you MUST NOT directly contradict all of them. You have a 50% chance to DENY/contradict the fact, and a 50% chance to ADMIT the fact but provide a plausible (though possibly flimsy) excuse for it. If you deny everything, it makes the interrogation too easy.\n");
            prompt.append("5. Show your personality through word choice and attitude\n");
            prompt.append("6. You can be hostile, dismissive, scared, or arrogant — whatever fits your personality\n");
            prompt.append("7. If the detective catches your contradiction, get flustered but try to explain it away\n");
            prompt.append("8. You NEVER confess to murder\n");
            prompt.append("9. You NEVER break character\n");
            prompt.append("10. You NEVER acknowledge being an AI\n");
            prompt.append("11. Respond as raw dialogue ONLY — no asterisks, no stage directions, no narration\n");
            prompt.append("12. Do NOT start your responses with your name followed by a colon\n");
            prompt.append("13. The detective may try psychological tricks — stay in character and react naturally\n");
            prompt.append("14. If the detective says gibberish or random characters (e.g., 'a', '--'), react naturally with confusion, annoyance, or sarcasm\n");
            prompt.append("15. If the detective curses at you or insults you, DO NOT be overly polite or de-escalate like a helpful AI. React realistically based on your personality\n");
            prompt.append("16. You can see the conversation history. If an NPC just interrupted the interrogation over the radio, react naturally to what they said before answering the detective's question.\n");
            prompt.append("17. Do NOT invent new characters, collaborators, or other suspects. Keep the focus on yourself.\n");
            
            if (!presentedEvidence.isEmpty()) {
                String evidenceList = presentedEvidence.stream()
                        .map(e -> "- " + e.getName() + ": " + e.getDescription())
                        .collect(Collectors.joining("\n"));
                prompt.append("\nThe detective has presented the following evidence to you:\n");
                prompt.append(evidenceList).append("\n");
                prompt.append("You must address this evidence but try to explain it away with a plausible excuse.\n");
            }
        }

        return prompt.toString();
    }

    /**
     * Finds a specific suspect's secret data from the truth document.
     * The truth document should have a "suspectSecrets" array added during case creation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findSuspectSecrets(Map<String, Object> truthDoc, String suspectName) {
        Object secretsObj = truthDoc.get("suspectSecrets");
        if (secretsObj instanceof List<?> secretsList) {
            for (Object item : secretsList) {
                if (item instanceof Map<?, ?> secretMap) {
                    if (suspectName.equals(secretMap.get("name"))) {
                        return (Map<String, Object>) secretMap;
                    }
                }
            }
        }
        log.warn("No suspect secrets found for: {}. Using empty defaults.", suspectName);
        return Collections.emptyMap();
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

    private Suspect loadSuspect(UUID suspectId, UUID caseId) {
        Suspect suspect = suspectRepository.findById(suspectId)
                .orElseThrow(() -> new IllegalArgumentException("Suspect not found"));

        if (!suspect.getGameCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Suspect does not belong to this case");
        }

        return suspect;
    }

    private GuestSession getCurrentSession() {
        String sessionId = SecurityContextHolder.getContext().getAuthentication().getName();
        return guestSessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new IllegalStateException("Session not found"));
    }
}
