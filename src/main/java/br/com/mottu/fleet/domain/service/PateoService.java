package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PateoService {
    List<Pateo> listarTodosAtivos();
    Pateo criarPateo(OnboardingRequest request, UsuarioAdmin adminResponsavel);
    Pateo buscarDetalhesDoPateo(UUID pateoId, UsuarioAdmin adminLogado);
    Optional<Pateo> buscarPorIdComZonas(UUID pateoId);
}