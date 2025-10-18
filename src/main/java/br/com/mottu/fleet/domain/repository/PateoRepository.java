package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;
import java.util.Optional;


/**
 * Repositório para a entidade Pateo.
 */
public interface PateoRepository extends JpaRepository<Pateo, UUID> {

    /**
     * Busca todos os pátios gerenciados por um ID de administrador específico.
     * @param gerenciadoPorId O UUID do UsuarioAdmin.
     * @return Uma lista de Patios.
     */
    List<Pateo> findAllByGerenciadoPorId(UUID gerenciadoPorId);


    /**
     * Busca todos os pátios filtrando por status.
     * @param status O Status (ATIVO, REMOVIDO, etc.).
     * @return Uma lista de Patios.
     */
    List<Pateo> findAllByStatus(Status status);


    /**
     * Busca um pátio pelo ID com a coleção de Zonas associadas.
     * @param id O UUID do pátio.
     * @return Um Optional contendo o Pateo com suas Zonas.
     */
    @Query("SELECT p FROM Pateo p LEFT JOIN FETCH p.zonas WHERE p.id = :id")
    Optional<Pateo> findPateoWithZonasById(@Param("id") UUID id);


    /**
     * Busca um pátio pelo ID e carrega todas as suas coleções (Zonas e Funcionarios) 
     * em uma única query. (tela de "Detalhes do Pátio" do SUPER ADMIN).
     * @param id O UUID do pátio.
     * @return Um Optional contendo o Pateo com todos os detalhes.
     */
    @Query("SELECT p FROM Pateo p LEFT JOIN FETCH p.zonas LEFT JOIN FETCH p.funcionarios WHERE p.id = :id")
    Optional<Pateo> findPateoWithDetailsById(@Param("id") UUID id);


    /**
     * Busca o primeiro pátio gerenciado por um ID de administrador.
     * Usado para encontrar o pátio de um PATEO_ADMIN (que só gerencia um).
     * @param adminId O UUID do UsuarioAdmin.
     * @return Um Optional contendo o Pateo.
     */
    Optional<Pateo> findFirstByGerenciadoPorId(UUID adminId);

}