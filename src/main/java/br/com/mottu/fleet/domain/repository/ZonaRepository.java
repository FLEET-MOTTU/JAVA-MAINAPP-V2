package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ZonaRepository extends JpaRepository<Zona, UUID> {
}