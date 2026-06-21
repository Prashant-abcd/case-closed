package com.caseclosed.backend.service;

import com.caseclosed.backend.entity.Conversation;
import com.caseclosed.backend.entity.Evidence;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicEvidenceService {

    private final ClaudeService claudeService;
    private final EvidenceRepository evidenceRepository;

    @Transactional
    public void evaluateAndGenerateEvidence(GameCase gameCase, Conversation conversation, String latestDetectiveMessage) {
        try {
            // Fetch existing evidence
            List<Evidence> existingEvidence = evidenceRepository.findByGameCaseId(gameCase.getId());
            
            if (existingEvidence.size() > 10) {
                return; // Hard cap on evidence per case to avoid DB bloat
            }
            
            StringBuilder evidenceList = new StringBuilder();
            if (!existingEvidence.isEmpty()) {
                evidenceList.append("PREVIOUSLY GENERATED EVIDENCE:\n");
                for (Evidence e : existingEvidence) {
                    evidenceList.append("- ").append(e.getName()).append(": ").append(e.getDescription()).append("\n");
                }
            } else {
                evidenceList.append("PREVIOUSLY GENERATED EVIDENCE: None yet.\n");
            }

            StringBuilder chatHistory = new StringBuilder();
            List<Map<String, Object>> messages = conversation.getMessages();
            // Send complete chat history
            for (Map<String, Object> msg : messages) {
                chatHistory.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }

            // Extract context
            String caseBriefingStr = "";
            if (gameCase.getCaseBriefing() != null) {
                try {
                    caseBriefingStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(gameCase.getCaseBriefing());
                } catch (Exception ignored) {}
            }
            String truthDocumentStr = "";
            if (gameCase.getTruthDocument() != null) {
                try {
                    truthDocumentStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(gameCase.getTruthDocument());
                } catch (Exception ignored) {}
            }
            String backgroundCheck = conversation.getSuspect().getBackgroundCheck();

            String systemPrompt = "You are an AI game master for a detective game. " +
                    "Your job is to silently generate hidden clues in the database based on the detective's line of questioning and the suspect's response.\n" +
                    "You have access to the complete chat history, the Case Briefing, the Suspect's Background Check, and the SECRET TRUTH DOCUMENT (which confirms the suspect is the killer).\n\n" +
                    "CRITICAL GAME MECHANIC (THE DYNAMIC PROBABILITY RULE):\n" +
                    "Whenever the suspect makes a claim or denies something, you MUST generate a piece of physical or digital evidence (like CCTV, receipts, witness statements, emails, or logs) that relates to their claim.\n" +
                    "You must randomly decide whether this new evidence:\n" +
                    "A) Proves they are LYING (Against them - implicates them in the crime, catches them in a contradiction, or breaks their alibi) - 50% CHANCE\n" +
                    "B) Validates a MINOR DETAIL of their claim (In favor of them). CRITICAL INSTRUCTION: Because you know from the Truth Document that they are the actual killer, this 'In favor' evidence MUST NOT completely clear them of the murder! It should only validate a mundane or adjacent detail they claimed. ALSO, the EXACT truth of the murder is FLEXIBLE! Generate evidence that allows for multiple plausible theories (e.g., if they died of head trauma, generate evidence that could imply a direct strike OR a staged accident). Let the detective's questions shape the exact truth! - 50% CHANCE\n" +
                    "Make sure to adhere strictly to these probabilities (50% favor / 50% against)!\n\n" +
                    "RULES:\n" +
                    "1. Evaluate the entire context to make the evidence make logical sense. Do NOT generate evidence about made-up collaborators or new suspects. You MUST keep the evidence strictly appropriate for the era and setting of the case (e.g., if it's 1980, don't invent smartphones or modern CCTV).\n" +
                    "2. Try to generate evidence consisting of key places, locations, and specific times mentioned by the detective or suspect in the chat.\n" +
                    "3. CRITICAL: Review the PREVIOUSLY GENERATED EVIDENCE. Do NOT generate new evidence that contradicts what has already been established. Ensure the timeline and case remain logically solvable without plot holes!\n" +
                    "4. Do NOT generate evidence if the latest exchange is just a greeting or meaningless banter.\n" +
                    "5. If evidence is warranted, return a JSON object with 'name' (short, 2-5 words) and 'description' (1-2 sentences of what the evidence shows. THIS is where you reveal if it clears them or implicates them!).\n" +
                    "6. If NO evidence is warranted, return exactly the word 'NONE'.\n" +
                    "7. Only return valid JSON if generating evidence.\n\n" +
                    "Context:\n" +
                    "Case Briefing: " + caseBriefingStr + "\n" +
                    "Truth Document (SECRET): " + truthDocumentStr + "\n" +
                    "Background Check: " + backgroundCheck + "\n" +
                    evidenceList.toString();

            String userPrompt = String.format("Complete Chat History (latest exchange is at the bottom):\n%s\n\nBased on this latest exchange and the context, does this warrant new evidence? Remember to apply the 50/50 rule (50%% chance to defend them!) if you generate evidence. Return JSON or NONE.", chatHistory.toString());

            String response = claudeService.generate(systemPrompt, userPrompt, 500L).trim();

            if (response.toUpperCase().contains("NONE") && !response.startsWith("{")) {
                return;
            }

            com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cleanJson(response));
            if (json.has("name") && json.has("description")) {
                String name = json.get("name").asText();
                String description = json.get("description").asText();

                Evidence newEvidence = Evidence.builder()
                        .gameCase(gameCase)
                        .name("[NEW] " + name)
                        .description(description)
                        .isFound(false)
                        .isPresented(false)
                        .build();

                evidenceRepository.save(newEvidence);
                log.info("Dynamically generated new evidence: {}", name);
            }

        } catch (Exception e) {
            log.error("Failed to dynamically generate evidence", e);
        }
    }

    private String cleanJson(String rawJson) {
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        if (start < 0) return cleaned;
        return cleaned.substring(start);
    }
}
