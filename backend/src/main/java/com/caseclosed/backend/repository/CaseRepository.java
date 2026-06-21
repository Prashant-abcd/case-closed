package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.GameCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseRepository extends JpaRepository<GameCase, UUID> {}
