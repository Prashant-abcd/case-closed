package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.cases.EvidenceResponse;
import com.caseclosed.backend.dto.cases.EvidenceSearchRequest;
import com.caseclosed.backend.dto.cases.EvidenceSearchResponse;
import com.caseclosed.backend.entity.Evidence;
import com.caseclosed.backend.entity.GameCase;
import com.caseclosed.backend.enums.CaseStatus;
import com.caseclosed.backend.repository.CaseRepository;
import com.caseclosed.backend.repository.EvidenceRepository;
import com.caseclosed.backend.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final CaseRepository caseRepository;
    private final ClaudeService claudeService;
    private final ConversationRepository conversationRepository;

    @Transactional
    public EvidenceSearchResponse searchEvidence(UUID caseId, EvidenceSearchRequest request) {
        GameCase gameCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (gameCase.getStatus() != CaseStatus.active) {
            throw new IllegalStateException("Case is no longer active");
        }

        List<Evidence> allEvidence = evidenceRepository.findByGameCaseId(caseId);
        
        if (allEvidence.isEmpty()) {
            return EvidenceSearchResponse.builder()
                    .matchFound(false)
                    .strikeCount(gameCase.getStrikeCount())
                    .gameOver(false)
                    .build();
        }

        String evidenceListString = allEvidence.stream()
                .map(e -> String.format("ID: %s\nName: %s\nDescription: %s", e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = "You are an AI assistant for a detective game. The player is searching for hidden evidence.";
        String userPrompt = String.format("""
                The player searched for: '%s'

                Here are all the pieces of evidence in the database:
                %s

                Does the player's search query logically match any of these pieces of evidence? 
                RULES:
                1. BUILD A HIGHLY FLEXIBLE SEARCH SYSTEM. Even extremely vague, misspelled, or shorthand language should return the most relevant piece of evidence.
                2. Evaluate the semantic intent. If the player searches 'cameras', 'video', 'tape', or 'looking', match it to CCTV or security footage. If they search 'papers', 'records', 'files', match it to documents.
                3. Be extremely lenient. Partial matches, typos, and semantic synonyms MUST all count as a match.
                4. If there is a match, return ONLY the exact UUID string of the most relevant matching evidence. 
                5. If there is no match, return EXACTLY the word 'NONE'.
                CRITICAL INSTRUCTION: You must return ONLY the UUID or ONLY the word 'NONE'. Under no circumstances are you allowed to output explanations, reasoning, or conversation. DO NOT output "Search Response:". JUST the UUID or NONE.
                """, request.getQuery(), evidenceListString);

        String response = claudeService.generate(systemPrompt, userPrompt, 100L).trim();

        org.slf4j.LoggerFactory.getLogger(EvidenceService.class).info("Search Prompt: {}\nSearch Response: {}", userPrompt, response);

        if (response.toUpperCase().contains("NONE") && !response.toLowerCase().matches(".*[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}.*")) {
            return EvidenceSearchResponse.builder()
                    .matchFound(false)
                    .strikeCount(gameCase.getStrikeCount())
                    .gameOver(gameCase.getStrikeCount() >= 2)
                    .build();
        }

        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})").matcher(response.toLowerCase());
            if (matcher.find()) {
                UUID matchedId = UUID.fromString(matcher.group(1));
                Evidence matchedEvidence = allEvidence.stream()
                        .filter(e -> e.getId().equals(matchedId))
                        .findFirst()
                        .orElse(null);

                if (matchedEvidence != null) {
                    if (matchedEvidence.isFound()) {
                        return EvidenceSearchResponse.builder()
                                .matchFound(false)
                                .alreadyFound(true)
                                .strikeCount(gameCase.getStrikeCount())
                                .gameOver(false)
                                .build();
                    }

                    matchedEvidence.setFound(true);
                    evidenceRepository.save(matchedEvidence);
                    return EvidenceSearchResponse.builder()
                            .matchFound(true)
                            .evidence(EvidenceResponse.from(matchedEvidence))
                            .strikeCount(gameCase.getStrikeCount())
                            .gameOver(false)
                            .build();
                } else {
                    org.slf4j.LoggerFactory.getLogger(EvidenceService.class).warn("Claude matched UUID {}, but it's not in the evidence list!", matchedId);
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(EvidenceService.class).error("Error parsing Claude's search response", e);
        }

        return EvidenceSearchResponse.builder()
                .matchFound(false)
                .strikeCount(gameCase.getStrikeCount())
                .gameOver(gameCase.getStrikeCount() >= 2)
                .build();
    }

    @Transactional
    public com.caseclosed.backend.dto.cases.PresentEvidenceResponse presentEvidence(UUID caseId, UUID evidenceId) {
        GameCase gameCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        if (gameCase.getStatus() != CaseStatus.active) {
            throw new IllegalStateException("Case is no longer active");
        }

        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found"));

        if (!evidence.getGameCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Evidence does not belong to this case");
        }

        if (!evidence.isFound()) {
            throw new IllegalStateException("Cannot present evidence that hasn't been found");
        }
        
        if (evidence.isPresented()) {
             return com.caseclosed.backend.dto.cases.PresentEvidenceResponse.builder()
                     .success(true)
                     .strikeCount(gameCase.getStrikeCount())
                     .gameOver(false)
                     .build();
        }

        List<com.caseclosed.backend.entity.Conversation> convs = conversationRepository.findByGameCaseId(caseId);
        if (convs.isEmpty()) {
            throw new IllegalArgumentException("Conversation not found");
        }
        com.caseclosed.backend.entity.Conversation conv = convs.get(0);
        
        StringBuilder sb = new StringBuilder();
        if (conv.getMessages() != null) {
            for (java.util.Map<String, Object> msg : conv.getMessages()) {
                sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
        }
        String chatHistory = sb.toString();

        String systemPrompt = "You are evaluating if a player successfully caught a suspect in a lie using evidence.";
        String userPrompt = String.format("""
                EVIDENCE PRESENTED:
                Name: %s
                Description: %s
                
                SUSPECT CHAT HISTORY:
                %s
                
                Does this evidence explicitly contradict a lie the suspect told in the chat history, or is it so inherently damning that presenting it breaks their alibi?
                
                If YES, return a JSON object: {"success": true, "npcMessage": "The suspect is caught off guard..."}
                If NO, the suspect is annoyed. Return a JSON object: {"success": false, "npcMessage": "Sassy suspect response saying this is irrelevant and threatening to call a lawyer."}
                
                Respond ONLY with valid JSON.
                """, evidence.getName(), evidence.getDescription(), chatHistory);

        String response = claudeService.generate(systemPrompt, userPrompt, 500L).trim();
        
        boolean success = false;
        String npcMessage = "I have nothing to say to that.";
        try {
            com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cleanJson(response));
            success = json.has("success") && json.get("success").asBoolean();
            if (json.has("npcMessage")) npcMessage = json.get("npcMessage").asText();
        } catch (Exception e) {
            success = true; // Fallback to success on parsing error
        }

        if (success) {
            evidence.setPresented(true);
            evidenceRepository.save(evidence);
        } else {
            gameCase.setStrikeCount(gameCase.getStrikeCount() + 1);
            if (gameCase.getStrikeCount() >= 2) {
                gameCase.setStatus(CaseStatus.lost);
            }
            caseRepository.save(gameCase);
        }

        return com.caseclosed.backend.dto.cases.PresentEvidenceResponse.builder()
                .success(success)
                .npcMessage(npcMessage)
                .strikeCount(gameCase.getStrikeCount())
                .gameOver(gameCase.getStrikeCount() >= 2)
                .build();
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
    
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getFoundEvidence(UUID caseId) {
        return evidenceRepository.findByGameCaseIdAndIsFoundTrue(caseId).stream()
                .map(EvidenceResponse::from)
                .toList();
    }
}
