package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.AuthCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {
    Optional<AuthCode> findByCode(String code);

    @Modifying
    @Transactional
    @Query("DELETE FROM AuthCode a WHERE a.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") UUID funcionarioId);
    
}