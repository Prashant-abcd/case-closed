package com.caseclosed.backend.entity;

import com.caseclosed.backend.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GuestSession guestSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CaseStatus status = CaseStatus.active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "truth_document", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> truthDocument;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "case_briefing", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> caseBriefing;

    @Column(name = "accusation_suspect_id")
    private UUID accusationSuspectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> verdict;

    @Column(name = "strike_count", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int strikeCount = 0;
}
