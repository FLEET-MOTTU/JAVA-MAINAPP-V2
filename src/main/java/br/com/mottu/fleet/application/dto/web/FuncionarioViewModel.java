package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.Funcionario;

import java.util.Optional;

/**
 * ViewModel usado na tela de "Detalhes do Pátio" do Super Admin.
 * Encapsula a entidade Funcionario e a URL de um Magic Link válido (se existir).
 */
public record FuncionarioViewModel(
    Funcionario funcionario,
    Optional<String> magicLinkUrl
) {}