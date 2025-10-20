package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

/**
 * ViewModel para exibir um Administrador de Pátio junto com o nome do pátio que ele gerencia.
 * Usado exclusivamente na tela de listagem de usuários do Super Admin.
 */
public record AdminComPateoViewModel(
    UsuarioAdmin admin,
    String nomePateo
) {}