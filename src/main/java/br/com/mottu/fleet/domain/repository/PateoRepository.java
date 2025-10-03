package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface PateoRepository extends JpaRepository<Pateo, UUID> {
    List<Pateo> findAllByGerenciadoPorId(UUID gerenciadoPorId);
    List<Pateo> findAllByStatus(Status status);

    @Query("SELECT p FROM Pateo p LEFT JOIN FETCH p.zonas WHERE p.id = :id")
    Optional<Pateo> findPateoWithZonasById(@Param("id") UUID id);

    @Query("SELECT p FROM Pateo p LEFT JOIN FETCH p.zonas LEFT JOIN FETCH p.funcionarios WHERE p.id = :id")
    Optional<Pateo> findPateoWithDetailsById(@Param("id") UUID id);

    Optional<Pateo> findFirstByGerenciadoPorId(UUID adminId);

}