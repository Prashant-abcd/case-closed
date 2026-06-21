package com.caseclosed.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "folders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private GameCase gameCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspect_id", nullable = false)
    private Suspect suspect;

    @Column(columnDefinition = "TEXT")
    private String contradictions;

    @Column(name = "intent_to_murder", columnDefinition = "TEXT")
    private String intentToMurder;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
