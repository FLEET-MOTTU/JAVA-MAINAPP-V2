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


/**
 * Repositório para a entidade UsuarioAdmin (Super Admin e Admin de Pátio).
 */
public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, UUID> {

    /**
     * Busca um admin pelo seu e-mail (login).
     * @param email O e-mail do admin.
     * @return Um Optional contendo o UsuarioAdmin, se encontrado.
     */
    Optional<UsuarioAdmin> findByEmail(String email);


    /**
     * Busca uma lista de admins por Role e Status.
     * @param role A Role (ex: PATEO_ADMIN).
     * @param status O Status (ex: ATIVO).
     * @return Uma lista de UsuarioAdmin.
     */
    List<UsuarioAdmin> findAllByRoleAndStatus(Role role, Status status);


    /**
     * Busca uma página de admins por Role e Status.
     * Usado na tela de listagem de usuários do Super Admin.
     * @param role A Role.
     * @param status O Status.
     * @param pageable As informações de paginação.
     * @return Uma Page de UsuarioAdmin.
     */
    Page<UsuarioAdmin> findAllByRoleAndStatus(Role role, Status status, Pageable pageable);
    
}