package com.caseclosed.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private GameCase gameCase;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(name = "is_found", nullable = false)
    @Builder.Default
    private boolean isFound = false;

    @Column(name = "is_presented", nullable = false)
    @Builder.Default
    private boolean isPresented = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
