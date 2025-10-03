package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.AuthCode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {
    Optional<AuthCode> findByCode(String code);
}