package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Zona;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


/**
 * Repositório para a entidade Zona (áreas de trabalho dentro de um pátio).
 */
public interface ZonaRepository extends JpaRepository<Zona, UUID> {
}