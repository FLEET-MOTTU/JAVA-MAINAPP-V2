package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.api.FuncionarioUpdateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;

import java.util.List;
import java.util.UUID;

public interface FuncionarioService {
    record FuncionarioCriado(Funcionario funcionario, String magicLink) {}

    FuncionarioCriado criar(FuncionarioCreateRequest request, UsuarioAdmin adminLogado);
    List<Funcionario> listarPorAdminEfiltros(UsuarioAdmin adminLogado, Status status, Cargo cargo);
    Funcionario atualizar(UUID id, FuncionarioUpdateRequest request, UsuarioAdmin adminLogado);
    void desativar(UUID id, UsuarioAdmin adminLogado);

}
