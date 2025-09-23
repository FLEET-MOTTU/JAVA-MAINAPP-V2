package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.FuncionarioCreateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

public interface FuncionarioService {
    record FuncionarioCriado(Funcionario funcionario, String magicLink) {}

    FuncionarioCriado criar(FuncionarioCreateRequest request, UsuarioAdmin adminLogado);
}
