package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.TokenAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenAcessoRepository extends JpaRepository<TokenAcesso, UUID> {
    Optional<TokenAcesso> findByToken(String token);
}