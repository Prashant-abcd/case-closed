package com.caseclosed.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class NPCGenerationService {

    private final ClaudeService claudeService;
    private final Random random = new Random();

    private static final List<String> NPC_ROLES = List.of(
            "City Mayor",
            "Police Chief",
            "Forensics Expert",
            "City Councilman",
            "Press Secretary",
            "District Attorney"
    );

    /**
     * Generates a dynamic 1-2 sentence interruption from a random NPC.
     * Uses a fast, non-streaming Claude call.
     */
    public String generateInterruption() {
        String role = NPC_ROLES.get(random.nextInt(NPC_ROLES.size()));
        
        String systemPrompt = String.format("""
            You are a %s in a high-profile homicide investigation. 
            The detective is currently interrogating a prime suspect. 
            Interrupt the detective over the radio, phone, or intercom. 
            Drop a vague hint, make a political demand, show impatience, or threaten their job. 
            Invent a realistic full name for yourself. 
            Keep it strictly to 1 or 2 short sentences. 
            Respond with ONLY your raw dialogue, starting with your name and title.
            """, role);

        try {
            return claudeService.generate(systemPrompt, "Generate the interruption now.");
        } catch (Exception e) {
            log.error("Failed to generate NPC interruption", e);
            // Fallback interruption if API fails so the game doesn't crash
            return role + " Davis: Detective, wrap this up immediately. The press is breathing down my neck!";
        }
    }
}
