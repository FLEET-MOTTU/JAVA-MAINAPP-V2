package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.Funcionario;
import java.util.Optional;

public record FuncionarioViewModel(
    Funcionario funcionario,
    Optional<String> magicLinkUrl
) {}