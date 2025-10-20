package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.RefreshToken;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


/**
 * Repositório para a entidade RefreshToken (token de longa duração).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Busca um RefreshToken pelo seu valor de 'token' único.
     * @param token O token (um UUID em String).
     * @return Um Optional contendo o RefreshToken, se encontrado.
     */
    Optional<RefreshToken> findByToken(String token);


    /**
     * Deleta em massa todos os RefreshTokens associados a um funcionário.
     * Usado no fluxo de Hard Delete do Super Admin.
     * @param funcionarioId O ID do funcionário.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") UUID funcionarioId);
    
}