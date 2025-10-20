package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.AuthCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


/**
 * Repositório para a entidade AuthCode (código de troca de curta duração).
 */
public interface AuthCodeRepository extends JpaRepository<AuthCode, UUID> {

    /**
     * Busca um AuthCode pelo seu valor de 'code' único.
     * @param code O código de troca (um UUID em String).
     * @return Um Optional contendo o AuthCode, se encontrado.
     */
    Optional<AuthCode> findByCode(String code);


    /**
     * Deleta em massa todos os AuthCodes associados a um funcionário.
     * Usado no fluxo de Hard Delete do Super Admin.
     * @param funcionarioId O ID do funcionário.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuthCode a WHERE a.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") UUID funcionarioId);
    
}