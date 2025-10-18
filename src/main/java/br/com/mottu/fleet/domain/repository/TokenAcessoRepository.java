package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenAcessoRepository extends JpaRepository<TokenAcesso, UUID> {
    Optional<TokenAcesso> findByToken(String token);
    Optional<TokenAcesso> findFirstByFuncionarioAndUsadoIsFalseAndExpiraEmAfterOrderByCriadoEmDesc(Funcionario funcionario, Instant agora);
    Optional<TokenAcesso> findByTwilioMessageSid(String twilioMessageSid);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAcesso t WHERE t.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") UUID funcionarioId);    
}