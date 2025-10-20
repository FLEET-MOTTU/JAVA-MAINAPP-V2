package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.Funcionario;

/**
 * ViewModel para a tela de Gerenciamento Mestre de Funcionários do Super Admin.
 * Encapsula a entidade e a URL de foto acessível (seja ela a URL pública do Azurite
 * ou uma URL SAS de produção).
 */
public record FuncionarioMestreViewModel(
    Funcionario funcionario,
    String fotoUrlAcessivel
) {}