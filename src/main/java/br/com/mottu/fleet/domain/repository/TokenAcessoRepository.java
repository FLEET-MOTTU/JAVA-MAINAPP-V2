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
import java.util.Collection;
import java.util.List;


/**
 * Repositório para a entidade TokenAcesso (Magic Link de uso único).
 */
public interface TokenAcessoRepository extends JpaRepository<TokenAcesso, UUID> {

    /**
     * Busca um TokenAcesso pelo seu valor de 'token' (o UUID na URL).
     * @param token O UUID em String do Magic Link.
     * @return Um Optional contendo o TokenAcesso, se encontrado.
     */
    Optional<TokenAcesso> findByToken(String token);


    /**
     * Busca o último TokenAcesso VÁLIDO (não usado e não expirado) para um funcionário.
     * Usado pelo Super Admin para exibir o link de teste no painel.
     * @param funcionario A entidade do funcionário.
     * @param agora O timestamp atual (Instant.now()).
     * @return Um Optional contendo o TokenAcesso, se houver um válido.
     */
    Optional<TokenAcesso> findFirstByFuncionarioAndUsadoIsFalseAndExpiraEmAfterOrderByCriadoEmDesc(Funcionario funcionario, Instant agora);


    /**
     * Busca um TokenAcesso pelo ID da mensagem do Twilio (MessageSID).
     * @param twilioMessageSid O SID da mensagem.
     * @return Um Optional contendo o TokenAcesso.
     */
    Optional<TokenAcesso> findByTwilioMessageSid(String twilioMessageSid);


    /**
     * Deleta em massa todos os TokenAcesso associados a um funcionário.
     * Usado no fluxo de Hard Delete do Super Admin.
     * @param funcionarioId O ID do funcionário.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenAcesso t WHERE t.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") UUID funcionarioId);


    /**
     * Busca todos os tokens de acesso válidos (não usados, não expirados) para
     * uma coleção de funcionários em uma única query.
     * Otimizado para evitar o problema N+1.
     */
    @Query("SELECT t FROM TokenAcesso t " +
           "WHERE t.funcionario IN :funcionarios " +
           "AND t.usado = false " +
           "AND t.expiraEm > :agora " +
           "ORDER BY t.criadoEm DESC")
    List<TokenAcesso> findAllValidTokensByFuncionarioList(
            @Param("funcionarios") Collection<Funcionario> funcionarios,
            @Param("agora") Instant agora
    );
    
}