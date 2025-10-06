package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.api.FuncionarioUpdateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface FuncionarioService {
    record FuncionarioCriado(Funcionario funcionario, String magicLink) {}

    Funcionario criar(FuncionarioCreateRequest request, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException;
    List<Funcionario> listarPorAdminEfiltros(UsuarioAdmin adminLogado, Status status, Cargo cargo);
    Funcionario atualizar(UUID id, FuncionarioUpdateRequest request, UsuarioAdmin adminLogado);
    Funcionario atualizarFoto(UUID id, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException;
    void desativar(UUID id, UsuarioAdmin adminLogado);
    void reativar(UUID id, UsuarioAdmin adminLogado);
}
