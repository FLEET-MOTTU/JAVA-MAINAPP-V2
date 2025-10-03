package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.application.dto.api.PasswordChangeRequest;
import br.com.mottu.fleet.application.dto.web.AdminComPateoViewModel;
import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.web.UsuarioAdminUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.Optional;

public interface UsuarioAdminService {
    Page<AdminComPateoViewModel> listarAdminsDePateoPaginado(Status status, Pageable pageable);
    void desativarPorId(UUID id);
    Optional<UsuarioAdmin> buscarPorId(UUID id);
    void atualizar(UsuarioAdminUpdateRequest request);
    UsuarioAdmin criarAdminDePateo(OnboardingRequest request);
    void alterarSenha(UsuarioAdmin adminLogado, PasswordChangeRequest request);
    void reativarPorId(UUID id);
}