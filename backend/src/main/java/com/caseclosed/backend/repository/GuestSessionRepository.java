package com.caseclosed.backend.repository;

import com.caseclosed.backend.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuestSessionRepository extends JpaRepository<GuestSession, UUID> {}
