package com.caseclosed.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "suspects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suspect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private GameCase gameCase;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(name = "relationship_to_founder", nullable = false)
    private String relationshipToFounder;

    @Column(name = "avatar_seed", nullable = false)
    private String avatarSeed;

    @Column(name = "interrogation_count")
    @Builder.Default
    private int interrogationCount = 0;

    @Column(name = "is_killer", nullable = false)
    private boolean isKiller;

    @Column(name = "is_high_profile", nullable = false)
    @Builder.Default
    private boolean isHighProfile = false;

    @Column(name = "background_check", columnDefinition = "text")
    private String backgroundCheck;
}
