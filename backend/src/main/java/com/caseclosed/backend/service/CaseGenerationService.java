package com.caseclosed.backend.service;

import com.caseclosed.backend.dto.generation.GeneratedCaseData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseGenerationService {

    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a master mystery writer designing a case for a detective game.
            
            GAME MECHANICS:
            1. The player only sees the Case Briefing and the Suspect's Background Check initially.
            2. The player must deduce search terms from these initial documents to query the Evidence Database.
            3. The Evidence Database contains 5-6 hidden pieces of evidence (clues).
            4. Once the player finds the evidence, they confront the suspect to break their alibi.
            
            CRITICAL DIRECTIVE: DO NOT SPOIL THE MYSTERY.
            The Case Briefing and Background Check must set up a compelling mystery. They should NEVER explicitly state that the suspect is guilty, nor should they reveal the damning details of the evidence upfront. The initial documents should read like a cold, factual police report at the start of an investigation, where the suspect is just a person of interest.
            
            Respond ONLY with valid JSON.
            """;

    private static final String USER_PROMPT = """
            Generate a murder mystery case. 
            CRITICAL SETTING INSTRUCTION: The setting for this mystery MUST be:
            %s
            
            Return ONLY this JSON structure with no extra text:

            {
              "caseBriefing": {
                "headline": "Short punchy newspaper headline",
                "subheadline": "One line subtitle",
                "dateOfDeath": "The date",
                "locationOfDeath": "Where the body was found",
                "articleBody": "2-3 paragraph newspaper article about the death.",
                "founderName": "Victim's full name",
                "founderAge": 0,
                "founderTitle": "Victim's job title or role",
                "founderCompany": "Victim's workplace or organization",
                "companyValuation": "Brief note about victim's wealth or status",
                "pitchDeckSummary": "1-2 sentences about the victim's work",
                "policeStatement": "2-3 paragraph Police Chief statement. You MUST explicitly state the estimated time of death (e.g. 'died between 7 PM and 8 PM') and the method of death (e.g. 'died of blunt force trauma', 'poisoned'). Do NOT give away the murder weapon or damning evidence here. You MUST explicitly name the prime suspect (the person generated in the 'suspect' block below) and state they were brought in for questioning. Just state how the body was found, the condition, ETD, method of death, and that the prime suspect is being interrogated.",
                "whatWeKnow": ["Bullet point 1: A fact that ties the prime suspect to the crime/scene.", "Bullet point 2: Suspicious behavior or circumstantial evidence against the prime suspect.", "Bullet point 3: Another fact linking the prime suspect. (CRITICAL: Must be EXACTLY 3-4 points. These MUST directly relate to the prime suspect and explain WHY they are the prime suspect, not just random facts about the victim.)"],
                "politicalConnections": "1 sentence about the victim's connections"
              },
              "suspect": {
                "name": "Full name",
                "age": 0,
                "title": "Job title",
                "company": "Workplace",
                "relationshipToFounder": "How they knew the victim",
                "avatarSeed": "unique-short-string",
                "isKiller": true,
                "isHighProfile": false,
                "trueAlibi": "Where they actually were",
                "lieTheyTell": "The false story they give the detective",
                "contradiction": "ONE specific fact that breaks their lie.",
                "motiveToKill": "Why they might want the victim dead",
                "personality": "2-3 adjectives",
                "backgroundCheck": "A very brief, generic police file (1-2 sentences max). Give basic details like employment history or known habits. DO NOT include damning evidence or reveal their guilt."
              },
              "truthDocument": {
                "killerName": "Name of the actual killer",
                "weapon": "The murder weapon",
                "method": "1-2 sentences: how the murder happened",
                "detailedMotive": "2-3 sentences: why the killer did it",
                "crimeTimeline": "Brief timeline of the night",
                "coverUpActions": "What the killer did to hide evidence",
                "keyClues": ["clue 1", "clue 2", "clue 3"],
                "redHerrings": ["red herring 1", "red herring 2"]
              }
            }

            Rules:
            - Generate exactly 1 suspect. They MUST have isKiller: true.
            - avatarSeed values must be unique short strings.
            """;

    private static final String[] SETTINGS = {
        "A deep-sea research submarine",
        "A 1990s video game development studio",
        "An underground illegal casino",
        "A remote arctic research base",
        "A traveling circus",
        "A luxury space tourism flight",
        "A cursed archaeological dig site",
        "A competitive esports tournament",
        "A secretive culinary academy",
        "A heavily guarded art restoration lab",
        "A bustling 1920s speakeasy",
        "A high-security alpine ski resort",
        "An isolated lighthouse on a stormy night",
        "A futuristic cyberpunk robotics factory",
        "A grand masquerade ball at a billionaire's mansion",
        "A prestigious magical illusionist theater",
        "A moving luxury transcontinental train",
        "A botanical garden full of poisonous plants",
        "A high-stakes international chess tournament",
        "A film set for a troubled Hollywood blockbuster"
    };

    public GeneratedCaseData generateCase() {
        RuntimeException lastFailure = null;
        
        java.util.Random random = new java.util.Random();
        String selectedSetting = SETTINGS[random.nextInt(SETTINGS.length)];
        String finalUserPrompt = String.format(USER_PROMPT, selectedSetting);

        for (int attempt = 1; attempt <= 2; attempt++) {
            String rawJson = claudeService.generate(SYSTEM_PROMPT, finalUserPrompt, 8192L);

            try {
                GeneratedCaseData generatedCase = objectMapper.readValue(cleanJson(rawJson), GeneratedCaseData.class);
                validateGeneratedCase(generatedCase);
                return generatedCase;
            } catch (Exception e) {
                lastFailure = new RuntimeException("Failed to parse case generation response: " + e.getMessage(), e);
                log.warn("Case generation attempt {} returned invalid JSON", attempt, e);
            }
        }

        throw lastFailure;
    }

    private String cleanJson(String rawJson) {
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }

        int start = cleaned.indexOf('{');
        if (start < 0) {
            return cleaned;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return cleaned.substring(start, i + 1);
                }
            }
        }

        return cleaned.substring(start);
    }

    private void validateGeneratedCase(GeneratedCaseData generatedCase) {
        if (generatedCase.getCaseBriefing() == null || generatedCase.getTruthDocument() == null) {
            throw new IllegalStateException("Generated case is missing required documents");
        }
        if (generatedCase.getSuspect() == null) {
            throw new IllegalStateException("Generated case must contain exactly 1 suspect");
        }
        if (!generatedCase.getSuspect().isKiller()) {
            throw new IllegalStateException("The suspect must be the killer");
        }
    }
}
