package com.caseclosed.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "npc_interruptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpcInterruption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private GameCase gameCase;

    @Column(name = "npc_name", nullable = false)
    private String npcName;

    @Column(name = "npc_title", nullable = false)
    private String npcTitle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "npc_conversation", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> npcConversation = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "triggered_at", updatable = false)
    private LocalDateTime triggeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspect_id", nullable = false)
    private Suspect suspect;
}
