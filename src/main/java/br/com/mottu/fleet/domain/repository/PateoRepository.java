package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface PateoRepository extends JpaRepository<Pateo, UUID> {
    List<Pateo> findAllByGerenciadoPorId(UUID gerenciadoPorId);
    List<Pateo> findAllByStatus(Status status);
}