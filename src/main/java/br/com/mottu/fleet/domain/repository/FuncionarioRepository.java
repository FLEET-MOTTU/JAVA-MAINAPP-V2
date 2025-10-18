package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Funcionario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;
import java.util.List;


/**
 * Repositório para a entidade Funcionario.
 * Estende JpaSpecificationExecutor para permitir buscas dinâmicas (filtros).
 */
public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID>, JpaSpecificationExecutor<Funcionario> {

    /**
     * Busca um funcionário pelo e-mail único.
     * @param email O e-mail do funcionário.
     * @return Um Optional contendo o Funcionario, se encontrado.
     */
    Optional<Funcionario> findByEmail(String email);


    /**
     * Busca todos os funcionários (Super Admin)
     * a entidade Pateo associada para evitar queries N+1.
     * @return Uma lista de todos os Funcionarios com seus Patios.
     */
    @Query("SELECT f FROM Funcionario f JOIN FETCH f.pateo p ORDER BY p.nome, f.nome")
    List<Funcionario> findAllWithPateo();


    /**
     * Busca todos os funcionários de um pátio específico (Super Admin)
     * @param pateoId O ID do pátio para filtrar.
     * @return Uma lista de Funcionarios daquele pátio.
     */
    @Query("SELECT f FROM Funcionario f JOIN FETCH f.pateo p WHERE p.id = :pateoId ORDER BY f.nome")
    List<Funcionario> findAllByPateoIdWithPateo(@Param("pateoId") UUID pateoId);

}