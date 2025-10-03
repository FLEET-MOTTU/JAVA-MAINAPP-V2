package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, UUID> {
    Optional<UsuarioAdmin> findByEmail(String email);
    List<UsuarioAdmin> findAllByRoleAndStatus(Role role, Status status);
    Page<UsuarioAdmin> findAllByRoleAndStatus(Role role, Status status, Pageable pageable);
}