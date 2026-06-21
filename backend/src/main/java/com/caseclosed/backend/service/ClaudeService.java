package com.caseclosed.backend.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class ClaudeService {

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.model}")
    private String model;

    private AnthropicClient client;

    @PostConstruct
    public void init() {
        client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Non-streaming generation (used for truth document generation).
     */
    public String generate(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, 4096L);
    }

    public String generate(String systemPrompt, String userPrompt, long maxTokens) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        Message response = client.messages().create(params);

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", String::concat);
    }

    /**
     * Streaming generation for interrogation.
     * Sends text chunks to the onToken callback as Claude generates them.
     *
     * @param systemPrompt  The suspect roleplay system prompt
     * @param history       Conversation history as [{role, content}] pairs
     * @param onToken       Called with each text chunk
     * @param onComplete    Called when streaming finishes
     * @param onError       Called if an error occurs
     */
    public void streamInterrogationResponse(
            String systemPrompt,
            List<Map<String, String>> history,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        try {
            // Build message params with conversation history
            MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(2048L)
                    .system(systemPrompt);

            // Add each message from conversation history
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    paramsBuilder.addUserMessage(content);
                } else if ("assistant".equals(role)) {
                    paramsBuilder.addAssistantMessage(content);
                }
            }

            MessageCreateParams params = paramsBuilder.build();

            // Stream the response using the Anthropic SDK
            try (StreamResponse<RawMessageStreamEvent> streamResponse =
                         client.messages().createStreaming(params)) {

                streamResponse.stream()
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .forEach(textDelta -> onToken.accept(textDelta.text()));
            }

            onComplete.run();

        } catch (Exception e) {
            log.error("Claude streaming failed: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }
}
