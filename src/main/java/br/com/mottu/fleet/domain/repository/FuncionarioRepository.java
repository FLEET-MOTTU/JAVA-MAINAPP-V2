package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID>, JpaSpecificationExecutor<Funcionario> {
    Optional<Funcionario> findByEmail(String email);

    @Query("SELECT f FROM Funcionario f JOIN FETCH f.pateo p ORDER BY p.nome, f.nome")
    List<Funcionario> findAllWithPateo();

    @Query("SELECT f FROM Funcionario f JOIN FETCH f.pateo p WHERE p.id = :pateoId ORDER BY f.nome")
    List<Funcionario> findAllByPateoIdWithPateo(@Param("pateoId") UUID pateoId);

}