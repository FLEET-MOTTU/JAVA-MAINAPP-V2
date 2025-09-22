package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, UUID> {
    Optional<UsuarioAdmin> findByEmail(String email);
    List<UsuarioAdmin> findAllByRoleAndStatus(String role, String status);
}