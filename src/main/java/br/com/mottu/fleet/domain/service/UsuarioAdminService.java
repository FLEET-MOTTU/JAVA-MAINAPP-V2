package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.application.dto.PasswordChangeRequest;
import br.com.mottu.fleet.application.dto.UsuarioAdminUpdateRequest;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface UsuarioAdminService {
    List<UsuarioAdmin> listarAdminsDePateo();
    void desativarPorId(UUID id);
    Optional<UsuarioAdmin> buscarPorId(UUID id);
    void atualizar(UsuarioAdminUpdateRequest request);
    UsuarioAdmin criarAdminDePateo(OnboardingRequest request);
    void alterarSenha(UsuarioAdmin adminLogado, PasswordChangeRequest request);
}